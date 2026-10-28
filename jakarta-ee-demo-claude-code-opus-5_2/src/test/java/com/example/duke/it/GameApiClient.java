package com.example.duke.it;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import java.io.IOException;
import java.io.StringReader;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.lang.String.format;

/// Talks to the running application the way the browser does, cookie jar included, so the
/// session that owns a game is carried from one call to the next.
final class GameApiClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String JSON = "application/json";

    private final URI baseUri;
    private final HttpClient http;

    GameApiClient(final URI baseUri) {
        this.baseUri = baseUri;
        this.http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    /// A client with its own cookie jar, which is a second player as far as the server knows.
    GameApiClient asAnotherPlayer() {
        return new GameApiClient(baseUri);
    }

    ApiResponse startGame() throws IOException, InterruptedException {
        return post("api/games", null);
    }

    ApiResponse currentGame() throws IOException, InterruptedException {
        return get("api/games/current");
    }

    ApiResponse guess(final Integer value) throws IOException, InterruptedException {
        return post("api/games/current/guesses",
                value == null ? "{\"guess\":null}" : format("{\"guess\":%d}", value));
    }

    /// Sends a body the typed helpers cannot express, so the tests can push malformed JSON
    /// at the API the way a hand-written client might.
    ApiResponse postRaw(final String path, final String body) throws IOException, InterruptedException {
        return post(path, body);
    }

    ApiResponse postScore(final String name) throws IOException, InterruptedException {
        return post("api/leaderboard", Json.createObjectBuilder().add("name", name).build().toString());
    }

    ApiResponse leaderboard() throws IOException, InterruptedException {
        return get("api/leaderboard");
    }

    ApiResponse get(final String path) throws IOException, InterruptedException {
        return get(path, JSON);
    }

    ApiResponse get(final String path, final String accept) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(baseUri.resolve(path)).GET(), accept);
    }

    private ApiResponse post(final String path, final String body) throws IOException, InterruptedException {
        final var publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        return send(HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Content-Type", JSON)
                .POST(publisher));
    }

    private ApiResponse send(final HttpRequest.Builder builder) throws IOException, InterruptedException {
        return send(builder, JSON);
    }

    private ApiResponse send(final HttpRequest.Builder builder, final String accept)
            throws IOException, InterruptedException {
        final var response = http.send(
                builder.header("Accept", accept).timeout(REQUEST_TIMEOUT).build(),
                HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    /// One HTTP answer, with the body parsed on demand so a test can read it as an object or
    /// an array without caring which the endpoint returns.
    record ApiResponse(int status, String rawBody) {

        JsonObject asObject() {
            return read().asJsonObject();
        }

        JsonArray asArray() {
            return read().asJsonArray();
        }

        String message() {
            return asObject().getString("message", "");
        }

        private JsonStructure read() {
            if (rawBody == null || rawBody.isBlank()) {
                return JsonValue.EMPTY_JSON_OBJECT;
            }
            try (var reader = Json.createReader(new StringReader(rawBody))) {
                return reader.read();
            }
        }
    }
}
