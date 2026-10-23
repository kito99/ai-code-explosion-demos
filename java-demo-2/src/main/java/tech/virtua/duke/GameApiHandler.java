package tech.virtua.duke;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.Logger.Level.ERROR;
import static java.util.Objects.requireNonNull;
import static tech.virtua.duke.GameRoute.GUESS;
import static tech.virtua.duke.GameRoute.READ;
import static tech.virtua.duke.GameRoute.RESTART;
import static tech.virtua.duke.HttpStatus.INTERNAL_SERVER_ERROR;
import static tech.virtua.duke.HttpStatus.METHOD_NOT_ALLOWED;
import static tech.virtua.duke.HttpStatus.NOT_FOUND;
import static tech.virtua.duke.HttpStatus.OK;
import static tech.virtua.duke.HttpStatus.PAYLOAD_TOO_LARGE;
import static tech.virtua.duke.HttpStatus.SERVICE_UNAVAILABLE;

/// The JSON API. Every response is the player's whole game state, so the
/// browser only has to draw what it is told.
public final class GameApiHandler implements HttpHandler {

    /// A guess is at most a few characters, so anything larger is not a guess.
    static final int MAX_GUESS_BODY_BYTES = 16;

    /// What [HttpExchange#getResponseCode()] reports before headers are sent.
    private static final int RESPONSE_NOT_STARTED = -1;

    private static final Logger LOGGER = System.getLogger(GameApiHandler.class.getName());

    private final GameSessions sessions;

    public GameApiHandler(final GameSessions sessions) {
        this.sessions = requireNonNull(sessions);
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (final TooManySessionsException tooManySessions) {
            LOGGER.log(ERROR, tooManySessions.getMessage());
            reportFailure(exchange, SERVICE_UNAVAILABLE, Map.of(),
                    "Duke is busy with too many games right now. Try again in a moment.");
        } catch (final RuntimeException unexpected) {
            // The server stays up for other players, so the failure is reported
            // to this one and logged rather than propagated.
            LOGGER.log(ERROR, "Request to %s failed.".formatted(exchange.getRequestURI()), unexpected);
            reportFailure(exchange, INTERNAL_SERVER_ERROR, Map.of(),
                    "Something went wrong on Duke's end. Start a new game to keep playing.");
        } finally {
            exchange.close();
        }
    }

    /// The route is resolved before any session work, so a request to an address
    /// the game does not serve never claims a session slot.
    private void route(final HttpExchange exchange) throws IOException {
        final var path = exchange.getRequestURI().getPath();
        final var endpoint = GameRoute.forPath(path);
        if (endpoint.isEmpty()) {
            Responses.sendMessage(exchange, NOT_FOUND, Map.of(), "There is nothing at that address.");
            return;
        }

        final var route = endpoint.get();
        final var method = HttpMethod.from(exchange.getRequestMethod());
        if (method != route.method()) {
            Responses.sendMessage(exchange, METHOD_NOT_ALLOWED, route.allowHeader(),
                    "That request method is not allowed here.");
            return;
        }

        if (route == GUESS) {
            final var guess = readGuessBody(exchange);
            if (guess.isEmpty()) {
                Responses.sendMessage(exchange, PAYLOAD_TOO_LARGE, Map.of(), "That is far too long to be a guess.");
                return;
            }
            play(exchange, game -> game.submitGuess(guess.get()));
            return;
        }

        play(exchange, route == RESTART ? GuessingGame::restart : GuessingGame::state);
    }

    private void play(final HttpExchange exchange, final GameMove move) throws IOException {
        final var sessionId = SessionCookie.readFrom(exchange.getRequestHeaders())
                .orElseGet(SessionCookie::newSessionId);
        exchange.getResponseHeaders()
                .set(SessionCookie.setCookieHeaderName(), SessionCookie.headerValueFor(sessionId));
        Responses.sendJson(exchange, OK, GameStateJson.render(move.applyTo(sessions.gameFor(sessionId))));
    }

    /// Reads the body, or nothing at all when it is longer than a guess can be.
    /// One byte past the limit is enough to tell the two apart.
    private static Optional<String> readGuessBody(final HttpExchange exchange) throws IOException {
        final byte[] body;
        try (final var requestBody = exchange.getRequestBody()) {
            body = requestBody.readNBytes(MAX_GUESS_BODY_BYTES + 1);
        }
        return body.length > MAX_GUESS_BODY_BYTES
                ? Optional.empty()
                : Optional.of(new String(body, StandardCharsets.UTF_8));
    }

    /// Reports a failure only when the response has not already started, since
    /// a partly written response can no longer be replaced.
    private static void reportFailure(final HttpExchange exchange,
                                      final HttpStatus status,
                                      final Map<String, String> extraHeaders,
                                      final String message) throws IOException {
        if (exchange.getResponseCode() == RESPONSE_NOT_STARTED) {
            Responses.sendMessage(exchange, status, extraHeaders, message);
        }
    }

    /// What a request does to the player's game.
    @FunctionalInterface
    private interface GameMove {
        GameState applyTo(GuessingGame game);
    }
}
