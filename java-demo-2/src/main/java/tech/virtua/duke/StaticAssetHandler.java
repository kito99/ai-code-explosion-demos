package tech.virtua.duke;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static tech.virtua.duke.HttpMethod.GET;
import static tech.virtua.duke.HttpStatus.METHOD_NOT_ALLOWED;
import static tech.virtua.duke.HttpStatus.NOT_FOUND;
import static tech.virtua.duke.HttpStatus.OK;

/// Serves the front end from inside the jar. Only the listed files are
/// reachable, so no request path is ever turned into a file lookup.
public final class StaticAssetHandler implements HttpHandler {

    private static final String RESOURCE_DIRECTORY = "/web";
    private static final String INDEX_FILE = "index.html";
    private static final String ALLOW_HEADER = "Allow";

    private static final Map<String, String> ASSET_FILE_BY_PATH = Map.of(
            "/", INDEX_FILE,
            "/index.html", INDEX_FILE,
            "/styles.css", "styles.css",
            "/app.js", "app.js",
            "/duke-thinking.svg", "duke-thinking.svg",
            "/duke-wave.svg", "duke-wave.svg");

    private final Map<String, byte[]> contentByFile;

    private StaticAssetHandler(final Map<String, byte[]> contentByFile) {
        this.contentByFile = requireNonNull(contentByFile);
    }

    /// Reads every asset into memory up front, so a packaging mistake fails at
    /// startup instead of mid-game.
    ///
    /// @throws MissingAssetException when a listed asset is not on the classpath
    public static StaticAssetHandler loadFromClasspath() {
        final var content = ASSET_FILE_BY_PATH.values().stream()
                .distinct()
                .collect(toUnmodifiableMap(Function.identity(), StaticAssetHandler::readAsset));
        return new StaticAssetHandler(content);
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        try {
            final var path = exchange.getRequestURI().getPath();
            final var file = ASSET_FILE_BY_PATH.get(path);
            if (file == null) {
                Responses.sendMessage(exchange, NOT_FOUND, Map.of(), "There is nothing at that address.");
            } else if (HttpMethod.from(exchange.getRequestMethod()) != GET) {
                Responses.sendMessage(exchange, METHOD_NOT_ALLOWED, Map.of(ALLOW_HEADER, GET.name()),
                        "That request method is not allowed here.");
            } else {
                Responses.send(exchange, OK, MediaType.forFileName(file), Map.of(), contentByFile.get(file));
            }
        } finally {
            exchange.close();
        }
    }

    private static byte[] readAsset(final String file) {
        final var resourcePath = "%s/%s".formatted(RESOURCE_DIRECTORY, file);
        try (final var resource = StaticAssetHandler.class.getResourceAsStream(resourcePath)) {
            if (resource == null) {
                throw new MissingAssetException(resourcePath);
            }
            return resource.readAllBytes();
        } catch (final IOException readFailure) {
            throw new MissingAssetException(resourcePath, readFailure);
        }
    }
}
