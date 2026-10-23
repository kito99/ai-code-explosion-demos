package tech.virtua.duke;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static tech.virtua.duke.HttpMethod.GET;
import static tech.virtua.duke.HttpMethod.POST;

/// The addresses the game answers on, each with the one method it accepts.
public enum GameRoute {

    /// The player's current game, started if they do not have one.
    READ("/api/game", GET),

    /// Abandons the current game and starts a new one.
    RESTART("/api/game/new", POST),

    /// Submits a guess, sent as the plain-text request body.
    GUESS("/api/game/guess", POST);

    private static final String ALLOW_HEADER = "Allow";

    private final String path;
    private final HttpMethod method;

    GameRoute(final String path, final HttpMethod method) {
        this.path = path;
        this.method = method;
    }

    /// The address this route answers on.
    public String path() {
        return path;
    }

    /// The only method this route accepts.
    public HttpMethod method() {
        return method;
    }

    /// The `Allow` header to send with a method-not-allowed response.
    public Map<String, String> allowHeader() {
        return Map.of(ALLOW_HEADER, method.name());
    }

    /// The route serving `path`, if the game serves it at all.
    public static Optional<GameRoute> forPath(final String path) {
        requireNonNull(path);
        return Arrays.stream(values())
                .filter(candidate -> candidate.path.equals(path))
                .findFirst();
    }
}
