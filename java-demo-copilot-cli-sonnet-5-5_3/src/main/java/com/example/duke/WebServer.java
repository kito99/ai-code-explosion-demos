package com.example.duke;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTTP layer: static pages plus a small JSON API. */
public class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);
    private static final String GAME_ID_PARAMETER = "gameId";
    private static final String LEADERBOARD_PAGE = "/public/leaderboard.html";
    private static final String STATIC_ROOT = "/public";
    private static final String GENERIC_ERROR = "Something went wrong on our side. Please try again.";

    private final GameService gameService;
    private final Javalin javalin;

    public WebServer(final GameService gameService) {
        this.gameService = gameService;
        this.javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.staticFiles.add(STATIC_ROOT, Location.CLASSPATH);
            config.router.apiBuilder(() -> path("/api", () -> {
                post("/games", this::startGame);
                post("/games/{gameId}/guesses", this::submitGuess);
                post("/games/{gameId}/score", this::postScore);
                get("/leaderboard", this::leaderboard);
            }));
        });
        javalin.get("/leaderboard", this::leaderboardPage);
        registerErrorHandlers();
    }

    public WebServer start(final int port) {
        javalin.start(port);
        return this;
    }

    public int port() {
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }

    private void startGame(final Context context) {
        context.status(HttpStatus.CREATED)
                .json(GameView.of(gameService.startGame(), Optional.empty()));
    }

    private void submitGuess(final Context context) {
        final var gameId = context.pathParam(GAME_ID_PARAMETER);
        final var result = gameService.guess(gameId, context.bodyAsClass(GuessRequest.class).guess());
        context.json(GameView.of(gameService.find(gameId), Optional.of(result)));
    }

    private void postScore(final Context context) {
        final var entry = gameService.postScore(
                context.pathParam(GAME_ID_PARAMETER), context.bodyAsClass(ScoreRequest.class).name());
        context.status(HttpStatus.CREATED).json(Map.of(
                "message", String.format("Thanks, %s! Your score is on the leaderboard.", entry.playerName())));
    }

    private void leaderboard(final Context context) {
        final var entries = gameService.leaderboard();
        final List<Map<String, Object>> rows = IntStream.range(0, entries.size())
                .mapToObj(index -> Map.<String, Object>of(
                        "rank", index + 1,
                        "name", entries.get(index).playerName(),
                        "guesses", entries.get(index).guesses(),
                        "recordedAt", entries.get(index).recordedAt().toString()))
                .toList();
        context.header("Cache-Control", "no-store").json(Map.of("entries", rows));
    }

    private void leaderboardPage(final Context context) {
        context.contentType(ContentType.TEXT_HTML)
                .result(WebServer.class.getResourceAsStream(LEADERBOARD_PAGE));
    }

    private void registerErrorHandlers() {
        javalin.exception(InvalidGuessException.class, (exception, context) -> reject(context, HttpStatus.BAD_REQUEST, exception));
        javalin.exception(InvalidNameException.class, (exception, context) -> reject(context, HttpStatus.BAD_REQUEST, exception));
        javalin.exception(GameNotFoundException.class, (exception, context) -> reject(context, HttpStatus.NOT_FOUND, exception));
        javalin.exception(GameStateException.class, (exception, context) -> reject(context, HttpStatus.CONFLICT, exception));
        javalin.exception(PersistenceException.class, (exception, context) -> {
            LOGGER.error("Persistence failure", exception);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", GENERIC_ERROR));
        });
        javalin.exception(BadRequestResponse.class, (exception, context) ->
                context.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "That request could not be read. Please try again.")));
    }

    private static void reject(final Context context, final HttpStatus status, final DukeException exception) {
        context.status(status).json(Map.of("error", exception.getMessage()));
    }

    /** Guess payload; the value is kept as text so non-numeric input can be rejected politely. */
    public record GuessRequest(String guess) {
    }

    public record ScoreRequest(String name) {
    }
}
