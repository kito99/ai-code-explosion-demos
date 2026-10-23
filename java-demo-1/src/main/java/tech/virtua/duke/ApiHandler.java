package tech.virtua.duke;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The game's JSON API.
 *
 * <ul>
 *   <li>{@code GET  /api/game} — the current game, starting one if the player has none</li>
 *   <li>{@code POST /api/game/new} — abandon the current game and start a fresh one</li>
 *   <li>{@code POST /api/game/guess} — submit {@code guess=<n>} as a form field</li>
 * </ul>
 */
public final class ApiHandler implements HttpHandler {

    static final String COOKIE_NAME = "duke_game";
    private static final int MAX_BODY_BYTES = 1024;

    private final GameStore store;

    public ApiHandler(GameStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                route(exchange);
            } catch (RuntimeException e) {
                if (exchange.getResponseCode() == -1) {
                    sendJson(exchange, 500, null,
                            Json.object(Json.member("error", "Something went wrong on the server.")));
                }
            }
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String sessionId = sessionIdFrom(exchange);

        switch (path) {
            case "/api/game" -> {
                if (requireMethod(exchange, method, "GET")) {
                    send(exchange, store.apply(sessionId, false, Game::state));
                }
            }
            case "/api/game/new" -> {
                if (requireMethod(exchange, method, "POST")) {
                    send(exchange, store.apply(sessionId, true, Game::state));
                }
            }
            case "/api/game/guess" -> {
                if (requireMethod(exchange, method, "POST")) {
                    String guess = formField(readBody(exchange), "guess");
                    send(exchange, store.apply(sessionId, false, game -> game.guess(guess)));
                }
            }
            default -> sendJson(exchange, 404, null,
                    Json.object(Json.member("error", "No such endpoint: " + path)));
        }
    }

    private boolean requireMethod(HttpExchange exchange, String actual, String allowed) throws IOException {
        if (allowed.equals(actual)) {
            return true;
        }
        exchange.getResponseHeaders().set("Allow", allowed);
        sendJson(exchange, 405, null, Json.object(Json.member("error", "Use " + allowed + " for this endpoint.")));
        return false;
    }

    private void send(HttpExchange exchange, GameStore.Result result) throws IOException {
        sendJson(exchange, 200, result.sessionId(), result.state().toJson());
    }

    private void sendJson(HttpExchange exchange, int status, String sessionId, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if (sessionId != null) {
            exchange.getResponseHeaders().set("Set-Cookie",
                    COOKIE_NAME + "=" + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
        }
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    /** Returns null for a body too large to be a guess; truncating one could turn junk into a real guess. */
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] bytes = in.readNBytes(MAX_BODY_BYTES + 1);
            return bytes.length > MAX_BODY_BYTES ? null : new String(bytes, StandardCharsets.UTF_8);
        }
    }

    static String formField(String body, String name) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            if (decode(key).equals(name)) {
                return eq < 0 ? "" : decode(pair.substring(eq + 1));
            }
        }
        return null;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // A malformed escape is just an invalid guess; let the game reject it.
            return value;
        }
    }

    static String sessionIdFrom(HttpExchange exchange) {
        List<String> headers = exchange.getRequestHeaders().get("Cookie");
        if (headers == null) {
            return null;
        }
        for (String header : headers) {
            for (String cookie : header.split(";")) {
                String trimmed = cookie.strip();
                if (trimmed.startsWith(COOKIE_NAME + "=")) {
                    String value = trimmed.substring(COOKIE_NAME.length() + 1);
                    return isValidSessionId(value) ? value : null;
                }
            }
        }
        return null;
    }

    /** Session ids are UUIDs we minted; anything else is ignored so the map cannot be seeded. */
    private static boolean isValidSessionId(String value) {
        if (value.length() != 36) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                if (c != '-') {
                    return false;
                }
            } else if (!hex) {
                return false;
            }
        }
        return true;
    }
}
