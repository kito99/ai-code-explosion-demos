package com.example.guessthenumber.it;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.io.StringReader;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/// One player talking to the API. Each client keeps its own cookies, so two clients are two
/// independent sessions with their own games.
final class GameApiClient {

    private static final String JSON = "application/json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final URI baseUri;

    GameApiClient(final URI baseUri) {
        this.baseUri = baseUri;
    }

    ApiResponse get(final String path) throws IOException, InterruptedException {
        return send(request(path).GET());
    }

    ApiResponse post(final String path) throws IOException, InterruptedException {
        return send(request(path).POST(HttpRequest.BodyPublishers.noBody()));
    }

    ApiResponse post(final String path, final JsonObject body) throws IOException, InterruptedException {
        return send(request(path)
                .header("Content-Type", JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
    }

    private HttpRequest.Builder request(final String path) {
        return HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Accept", JSON)
                .timeout(REQUEST_TIMEOUT);
    }

    private ApiResponse send(final HttpRequest.Builder builder) throws IOException, InterruptedException {
        final var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    /// A status code and a body, with the body parsed on demand.
    record ApiResponse(int status, String body) {

        JsonObject asObject() {
            try (var reader = Json.createReader(new StringReader(body))) {
                return reader.readObject();
            }
        }

        JsonArray asArray() {
            try (var reader = Json.createReader(new StringReader(body))) {
                return reader.readArray();
            }
        }

        String message() {
            return asObject().getString("message");
        }
    }
}
