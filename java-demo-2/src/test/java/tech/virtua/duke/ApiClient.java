package tech.virtua.duke;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

/// A single browser talking to the game. Cookies are kept per instance, so two
/// clients behave like two different players.
final class ApiClient implements AutoCloseable {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .connectTimeout(TIMEOUT)
            .build();

    private final String baseUrl;

    ApiClient(final int port) {
        this.baseUrl = "http://localhost:%d".formatted(port);
    }

    ApiResponse currentGame() throws IOException, InterruptedException {
        return send(request(GameRoute.READ.path()).GET());
    }

    ApiResponse newGame() throws IOException, InterruptedException {
        return send(request(GameRoute.RESTART.path()).POST(noBody()));
    }

    ApiResponse guess(final String rawGuess) throws IOException, InterruptedException {
        return send(request(GameRoute.GUESS.path()).POST(ofString(rawGuess)));
    }

    ApiResponse get(final String path) throws IOException, InterruptedException {
        return send(request(path).GET());
    }

    ApiResponse post(final String path) throws IOException, InterruptedException {
        return send(request(path).POST(noBody()));
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private HttpRequest.Builder request(final String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(TIMEOUT);
    }

    private ApiResponse send(final HttpRequest.Builder builder) throws IOException, InterruptedException {
        final var response = httpClient.send(builder.build(), BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(),
                response.headers().firstValue("Content-Type").orElse(""),
                response.headers().firstValue("Allow").orElse(""),
                response.body());
    }
}
