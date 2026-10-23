package tech.virtua.duke.it;

import jakarta.json.Json;
import jakarta.json.JsonException;

import java.io.IOException;
import java.io.StringReader;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/// One browser, cookies and all, playing the game over real HTTP.
final class GameClient {

    private static final String DEFAULT_PORT = "9080";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .connectTimeout(TIMEOUT)
            .build();

    static String baseUrl() {
        return "http://localhost:" + System.getProperty("http.port", DEFAULT_PORT);
    }

    GameResponse currentGame() {
        return send(request("/api/game").GET());
    }

    GameResponse newGame() {
        return send(request("/api/game/new").POST(HttpRequest.BodyPublishers.noBody()));
    }

    GameResponse guess(final String rawGuess) {
        return submit("{\"guess\":" + quoted(rawGuess) + "}");
    }

    /// Posts a body to the guess endpoint exactly as given, valid JSON or not.
    GameResponse submit(final String body) {
        return send(request("/api/game/guess")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    HttpResponse<String> fetch(final String path) {
        return sendRaw(request(path).GET());
    }

    private static String quoted(final String rawGuess) {
        return rawGuess == null ? "null" : "\"" + rawGuess.replace("\"", "\\\"") + "\"";
    }

    private HttpRequest.Builder request(final String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/json");
    }

    private GameResponse send(final HttpRequest.Builder request) {
        final var response = sendRaw(request);
        try (var reader = Json.createReader(new StringReader(response.body()))) {
            return new GameResponse(response.statusCode(), reader.readObject());
        } catch (final JsonException notJson) {
            // Usually means something other than this application is holding the port.
            throw new IllegalStateException(
                    "The server at %s answered %d with something that is not JSON. Body: %s"
                            .formatted(baseUrl(), response.statusCode(), response.body()), notJson);
        }
    }

    private HttpResponse<String> sendRaw(final HttpRequest.Builder request) {
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while talking to the game.", interrupted);
        } catch (final IOException failure) {
            throw new IllegalStateException("Could not reach the game at " + baseUrl() + ".", failure);
        }
    }
}
