package tech.virtua.duke;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Serves the front-end straight out of the jar's {@code /web} folder. */
public final class StaticHandler implements HttpHandler {

    private static final String ROOT = "web";
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "ico", "image/x-icon",
            "webmanifest", "application/manifest+json");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String resource = resourceFor(exchange.getRequestURI().getPath());
            if (resource == null) {
                send(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
            }

            try (InputStream in = StaticHandler.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    send(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                send(exchange, 200, contentType(resource), in.readAllBytes());
            }
        }
    }

    /** Maps a request path onto a classpath resource, or null if the path is not servable. */
    static String resourceFor(String path) {
        String cleaned = (path == null || path.isEmpty() || "/".equals(path)) ? "/index.html" : path;
        if (!cleaned.startsWith("/") || cleaned.contains("..") || cleaned.contains("//")) {
            return null;
        }
        return ROOT + cleaned;
    }

    private static String contentType(String resource) {
        int dot = resource.lastIndexOf('.');
        String extension = dot < 0 ? "" : resource.substring(dot + 1).toLowerCase();
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
