package tech.virtua.duke;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/// Writes responses. Shared by both handlers so the headers are set the same
/// way everywhere.
public final class Responses {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";

    /// The game is a live session on the server, so nothing it serves should be
    /// reused from a cache.
    private static final String NO_STORE = "no-store";

    /// Passing zero to `sendResponseHeaders` means "length unknown", so an empty
    /// body has to be announced as no body at all.
    private static final int NO_BODY = -1;

    private Responses() {
    }

    /// Sends `body` with the given status, content type, and any extra headers.
    public static void send(final HttpExchange exchange,
                            final HttpStatus status,
                            final MediaType mediaType,
                            final Map<String, String> extraHeaders,
                            final byte[] body) throws IOException {
        requireNonNull(body);
        final var headers = exchange.getResponseHeaders();
        headers.set(CONTENT_TYPE_HEADER, mediaType.headerValue());
        headers.set(CACHE_CONTROL_HEADER, NO_STORE);
        requireNonNull(extraHeaders).forEach(headers::set);
        exchange.sendResponseHeaders(status.code(), body.length == 0 ? NO_BODY : body.length);
        try (final var responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }

    /// Sends a JSON document.
    public static void sendJson(final HttpExchange exchange,
                                final HttpStatus status,
                                final String json) throws IOException {
        send(exchange, status, MediaType.JSON, Map.of(), json.getBytes(StandardCharsets.UTF_8));
    }

    /// Sends a JSON document carrying a single player-facing message.
    public static void sendMessage(final HttpExchange exchange,
                                   final HttpStatus status,
                                   final Map<String, String> extraHeaders,
                                   final String message) throws IOException {
        send(exchange, status, MediaType.JSON, extraHeaders,
                JsonWriter.object().put("message", message).build().getBytes(StandardCharsets.UTF_8));
    }
}
