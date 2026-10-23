package tech.virtua.guessthenumber.it;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * A browser-sized HTTP client for the game's API. It keeps cookies, so each instance
 * behaves like one player with their own server-side session.
 */
final class GameClient implements AutoCloseable {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .connectTimeout(TIMEOUT)
            .build();

    private final String baseUri;

    GameClient() {
        this.baseUri = "http://localhost:" + System.getProperty("http.port", "9080");
    }

    /**
     * Releases the client's selector and worker threads. Tests make a player apiece, so
     * leaving them open piles up threads over the course of a run.
     */
    @Override
    public void close() {
        http.shutdownNow();
    }

    /** One API response: the status code plus the parsed JSON body. */
    record Reply(int status, JsonObject body) {

        String message() {
            return body.getString("message", "");
        }

        String gameStatus() {
            return body.getString("status", "");
        }

        int guessesMade() {
            return body.getInt("guessesMade", -1);
        }

        int guessesRemaining() {
            return body.getInt("guessesRemaining", -1);
        }
    }

    Reply currentGame() {
        return sendJson(HttpRequest.newBuilder().GET());
    }

    Reply newGame() {
        return sendJson(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody()), "/api/game");
    }

    Reply guess(Object rawGuess) {
        String body = Json.createObjectBuilder()
                .add("guess", String.valueOf(rawGuess))
                .build()
                .toString();
        return postJson("/api/game/guesses", body);
    }

    /**
     * Posts a body the server cannot bind. The reply may not be JSON, so this hands back
     * the raw response and lets the test judge it.
     */
    HttpResponse<String> postRawGuessBody(String body) {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(baseUri + "/api/game/guesses"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    /** Fetches the game page itself, to confirm the static front end is served. */
    HttpResponse<String> homePage() {
        return send(HttpRequest.newBuilder()
                .uri(URI.create(baseUri + "/"))
                .timeout(TIMEOUT)
                .GET()
                .build());
    }

    private Reply postJson(String path, String body) {
        return sendJson(HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)), path);
    }

    private Reply sendJson(HttpRequest.Builder builder) {
        return sendJson(builder, "/api/game");
    }

    private Reply sendJson(HttpRequest.Builder builder, String path) {
        HttpResponse<String> response = send(builder
                .uri(URI.create(baseUri + path))
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .build());

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            return new Reply(response.statusCode(), reader.readObject());
        } catch (RuntimeException unparseable) {
            throw new AssertionError(
                    "Expected JSON from " + path + " but got HTTP " + response.statusCode()
                            + ": " + response.body(), unparseable);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted calling " + request.uri(), interrupted);
        } catch (Exception failed) {
            throw new AssertionError("Could not reach " + request.uri(), failed);
        }
    }
}
