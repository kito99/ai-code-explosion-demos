package com.example.duke.it;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.example.duke.game.GameRules.HIGHEST_NUMBER;
import static com.example.duke.game.GameRules.LOWEST_NUMBER;
import static com.example.duke.game.GameRules.MAX_GUESSES;
import static com.example.duke.leaderboard.LeaderboardService.TOP_ENTRY_COUNT;
import static com.example.duke.leaderboard.PlayerName.MAX_LENGTH;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Plays the real application over HTTP against a real PostgreSQL, both started by the test.
/// Nothing has to be running first beyond Docker.
///
/// The tests run in a fixed order because the leaderboard is shared state: the empty-board
/// check has to come before anything is posted, and the restart comes last.
@TestMethodOrder(OrderAnnotation.class)
class GuessTheNumberIT {

    private static final String DATABASE_NAME = "dukeguess";
    private static final String DATABASE_USER = "duke";
    private static final String DATABASE_PASSWORD = "duke";

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_WON = "WON";
    private static final String STATUS_LOST = "LOST";
    private static final String OUTCOME_CORRECT = "CORRECT";
    private static final String OUTCOME_TOO_LOW = "TOO_LOW";

    private static final String GUESSES_PATH = "api/games/current/guesses";

    private static final int OK = 200;
    private static final int CREATED = 201;
    private static final int BAD_REQUEST = 400;
    private static final int CONFLICT = 409;

    /// Enough rounds to fill the leaderboard past its limit and to make ties on guess count
    /// a certainty, so the ordering rules are actually exercised.
    private static final int ROUNDS_TO_PLAY = TOP_ENTRY_COUNT + 4;

    /// A game is lost by guessing a number already known to be wrong. That plan only fails
    /// when the very first guess happens to be right, which is worth retrying a few times.
    private static final int LOSS_ATTEMPTS = 40;

    private static final Duration TELEMETRY_TIMEOUT = Duration.ofSeconds(40);
    private static final Duration TELEMETRY_POLL = Duration.ofSeconds(1);

    private static PostgreSQLContainer<?> database;
    private static LibertyTestServer server;
    private static GameApiClient client;

    @BeforeAll
    static void startEverything() throws IOException, InterruptedException {
        database = new PostgreSQLContainer<>(
                DockerImageName.parse(System.getProperty("postgres.image", "postgres:18.6")))
                .withDatabaseName(DATABASE_NAME)
                .withUsername(DATABASE_USER)
                .withPassword(DATABASE_PASSWORD);
        database.start();

        server = new LibertyTestServer(
                Path.of(System.getProperty("liberty.installDirectory")),
                System.getProperty("liberty.serverName"),
                Integer.parseInt(System.getProperty("app.http.port")),
                Map.of("DB_HOST", database.getHost(),
                        "DB_PORT", String.valueOf(database.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)),
                        "DB_NAME", DATABASE_NAME,
                        "DB_USER", DATABASE_USER,
                        "DB_PASSWORD", DATABASE_PASSWORD));
        server.start();

        client = new GameApiClient(server.baseUri());
    }

    @AfterAll
    static void stopEverything() throws IOException, InterruptedException {
        if (server != null) {
            server.stop();
        }
        if (database != null) {
            database.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("an untouched leaderboard has nothing on it")
    void leaderboardStartsEmpty() throws IOException, InterruptedException {
        final var response = client.leaderboard();

        assertEquals(OK, response.status());
        assertTrue(response.asArray().isEmpty(), "a fresh database should have no entries");
    }

    @Test
    @Order(2)
    @DisplayName("a new game starts with every guess still available")
    void startsAGame() throws IOException, InterruptedException {
        final var response = client.startGame();
        final var game = response.asObject();

        assertEquals(CREATED, response.status());
        assertEquals(STATUS_IN_PROGRESS, game.getString("status"));
        assertEquals(0, game.getInt("guessesMade"));
        assertEquals(MAX_GUESSES, game.getInt("guessesRemaining"));
        assertFalse(game.containsKey("secretNumber"), "the secret number must stay hidden");
        assertFalse(game.getBoolean("canPostScore"));
        assertTrue(game.getString("message").contains("thinking of a number"));
    }

    @Test
    @Order(3)
    @DisplayName("the current game can be read back on the same session")
    void readsBackTheCurrentGame() throws IOException, InterruptedException {
        client.startGame();
        client.guess(LOWEST_NUMBER);

        final var game = client.currentGame().asObject();

        assertEquals(1, game.getInt("guessesMade"));
        assertEquals(LOWEST_NUMBER, game.getInt("lastGuess"));
    }

    @Test
    @Order(4)
    @DisplayName("finding the number wins and reveals it")
    void playsThroughToAWin() throws IOException, InterruptedException {
        final var win = winAGame(client);

        assertEquals(STATUS_WON, win.getString("status"));
        assertTrue(win.getBoolean("canPostScore"));
        assertEquals(win.getInt("lastGuess"), win.getInt("secretNumber"));
        assertTrue(win.getInt("guessesMade") >= 1 && win.getInt("guessesMade") <= MAX_GUESSES);
        assertTrue(win.getString("message").contains("You got it in"));
    }

    @Test
    @Order(5)
    @DisplayName("running out of guesses loses and reveals the number")
    void playsThroughToALoss() throws IOException, InterruptedException {
        final var loss = loseAGame(client);

        assertEquals(STATUS_LOST, loss.getString("status"));
        assertEquals(MAX_GUESSES, loss.getInt("guessesMade"));
        assertEquals(0, loss.getInt("guessesRemaining"));
        assertFalse(loss.getBoolean("canPostScore"));
        assertTrue(loss.containsKey("secretNumber"), "a loss reveals the number");
        assertTrue(loss.getString("message").contains("Out of guesses"));
    }

    @Test
    @Order(6)
    @DisplayName("a guess outside 1 to 10 is turned away without counting")
    void rejectsGuessesOutsideTheRange() throws IOException, InterruptedException {
        client.startGame();
        client.guess(LOWEST_NUMBER);

        for (final var rejected : List.of(0, -1, HIGHEST_NUMBER + 1, 99)) {
            final var response = client.guess(rejected);
            assertEquals(BAD_REQUEST, response.status(), () -> rejected + " should be refused");
            assertFalse(response.message().isBlank(), "a refusal needs a friendly explanation");
        }

        assertEquals(1, client.currentGame().asObject().getInt("guessesMade"),
                "a refused guess must not count against the limit");
    }

    @Test
    @Order(7)
    @DisplayName("a guess that is not a whole number is turned away too")
    void rejectsGuessesThatAreNotWholeNumbers() throws IOException, InterruptedException {
        client.startGame();

        final var rejected = List.of(
                "{\"guess\":null}",      // nothing sent
                "{\"guess\":\"seven\"}",   // not a number at all
                "{\"guess\":5.5}",       // a number, but not a whole one
                "{}",                   // the field left out
                "null",                 // a body that is not an object
                "");                    // no body at all
        for (final var body : rejected) {
            final var response = client.postRaw(GUESSES_PATH, body);
            assertEquals(BAD_REQUEST, response.status(), () -> format("[%s] should be refused", body));
            assertFalse(response.message().isBlank(), "a refusal needs a friendly explanation");
        }

        assertEquals(0, client.currentGame().asObject().getInt("guessesMade"),
                "none of those counted as a guess");
    }

    @Test
    @Order(8)
    @DisplayName("a finished game takes no more guesses")
    void rejectsGuessesOnAFinishedGame() throws IOException, InterruptedException {
        winAGame(client);

        final var response = client.guess(LOWEST_NUMBER);

        assertEquals(CONFLICT, response.status());
        assertTrue(response.message().contains("Start a new one"));
    }

    @Test
    @Order(9)
    @DisplayName("a loss puts nothing on the leaderboard")
    void refusesToPostWithoutAWin() throws IOException, InterruptedException {
        final var before = client.leaderboard().asArray().size();
        loseAGame(client);

        final var response = client.postScore("Nobody");

        assertEquals(CONFLICT, response.status());
        assertEquals(before, client.leaderboard().asArray().size(), "a loss records nothing");
    }

    @Test
    @Order(10)
    @DisplayName("a name that breaks the rules records nothing")
    void refusesAnUnacceptableName() throws IOException, InterruptedException {
        winAGame(client);
        final var before = client.leaderboard().asArray().size();

        for (final var rejected : List.of("", "   ", "D".repeat(MAX_LENGTH + 1))) {
            final var response = client.postScore(rejected);
            assertEquals(BAD_REQUEST, response.status(), () -> format("[%s] should be refused", rejected));
            assertTrue(response.message().contains("no more than"), "the name rules should be explained");
        }

        assertEquals(before, client.leaderboard().asArray().size(), "nothing is recorded until a name passes");
        assertEquals(CREATED, client.postScore("  Duke the Patient  ").status(),
                "a valid name should still be accepted afterwards");
    }

    @Test
    @Order(11)
    @DisplayName("a win goes on the leaderboard once and only once")
    void refusesToPostTheSameWinTwice() throws IOException, InterruptedException {
        final var win = winAGame(client);

        final var first = client.postScore("Duke");
        assertEquals(CREATED, first.status());
        assertEquals(win.getInt("guessesMade"), first.asObject().getInt("guessCount"),
                "the recorded count comes from the server's own record of the round");
        assertEquals("Duke", first.asObject().getString("name"));

        assertEquals(CONFLICT, client.postScore("Duke Again").status());
        assertFalse(client.currentGame().asObject().getBoolean("canPostScore"));
    }

    @Test
    @Order(12)
    @DisplayName("the leaderboard ranks by fewest guesses and shows at most ten")
    void ranksTheBestResultsAndCapsTheList() throws IOException, InterruptedException {
        for (var round = 0; round < ROUNDS_TO_PLAY; round++) {
            winAGame(client);
            assertEquals(CREATED, client.postScore("Player " + round).status());
        }

        final var entries = client.leaderboard().asArray().getValuesAs(JsonObject.class);

        assertEquals(TOP_ENTRY_COUNT, entries.size(), "the screen shows no more than ten");
        for (var index = 0; index < entries.size(); index++) {
            assertEquals(index + 1, entries.get(index).getInt("rank"), "ranks run from one upwards");
        }
        for (var index = 1; index < entries.size(); index++) {
            final var previous = entries.get(index - 1);
            final var current = entries.get(index);
            assertTrue(previous.getInt("guessCount") <= current.getInt("guessCount"),
                    "entries run from fewest guesses upwards");
            if (previous.getInt("guessCount") == current.getInt("guessCount")) {
                assertTrue(Instant.parse(previous.getString("recordedAt"))
                                .compareTo(Instant.parse(current.getString("recordedAt"))) <= 0,
                        "a tie goes to whichever was recorded first");
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("every player sees the same leaderboard")
    void sharesTheLeaderboardWithEveryPlayer() throws IOException, InterruptedException {
        final var otherPlayer = client.asAnotherPlayer();

        assertEquals(client.leaderboard().rawBody(), otherPlayer.leaderboard().rawBody());
        assertEquals(CONFLICT, otherPlayer.currentGame().status(),
                "a new session has no game of its own yet");
    }

    @Test
    @Order(14)
    @DisplayName("the app serves an OpenAPI definition and documentation for it")
    void servesAnOpenApiDefinitionAndDocumentation() throws IOException, InterruptedException {
        final var definition = client.get("openapi");
        assertEquals(OK, definition.status());
        for (final var expected : List.of("/games", "/games/current/guesses", "/leaderboard",
                "Guess the Number with Duke")) {
            assertTrue(definition.rawBody().contains(expected),
                    () -> "the definition should describe " + expected);
        }

        final var documentation = client.get("openapi/ui/", "text/html");
        assertEquals(OK, documentation.status());
        assertFalse(documentation.rawBody().isBlank(), "the browsable documentation should have content");
    }

    @Test
    @Order(15)
    @DisplayName("the game and its requests show up in the exported telemetry")
    void emitsTelemetryForTheGameAndItsRequests() throws IOException, InterruptedException {
        // Only what the log gains from here on counts, so an earlier test's telemetry cannot
        // stand in for this one's.
        final var alreadyLogged = server.consoleLog().length();

        client.startGame();
        client.guess(HIGHEST_NUMBER + 1);
        winAGame(client);
        client.postScore("Telemetry");

        // Span names are matched with the quotes and colon the console exporter prints around
        // them, so that game.started cannot stand in for the game.start span.
        final var expectedSpans = List.of("'game.start' :", "'game.guess' :",
                "'game.post.score' :", "'leaderboard.record' :");
        final var expectedMetrics = List.of("game.started", "game.won", "game.guess.accepted",
                "game.guess.rejected", "game.winning.guesses", "leaderboard.score.recorded");
        final var expected = Stream.concat(expectedSpans.stream(), expectedMetrics.stream()).toList();

        // Spans are batched and metrics are reported on an interval, so the log is given a
        // little while to catch up before the assertion runs.
        final var deadline = Instant.now().plus(TELEMETRY_TIMEOUT);
        var since = "";
        while (Instant.now().isBefore(deadline)) {
            since = server.consoleLog().substring(alreadyLogged);
            if (expected.stream().allMatch(since::contains)) {
                return;
            }
            Thread.sleep(TELEMETRY_POLL.toMillis());
        }

        final var lastLog = since;
        final var missing = expected.stream().filter(name -> !lastLog.contains(name)).toList();
        fail("the exported telemetry never mentioned " + missing);
    }

    @Test
    @Order(16)
    @DisplayName("leaderboard entries survive a restart of the app")
    void keepsLeaderboardEntriesAcrossARestart() throws IOException, InterruptedException {
        final var before = client.leaderboard().rawBody();
        assertFalse(client.leaderboard().asArray().isEmpty(), "there should be something to lose");

        server.restart();

        assertEquals(before, client.leaderboard().rawBody(), "the entries should still be there");
    }

    /// Plays a round to a win by narrowing the range with the feedback the server gives, and
    /// returns the winning state.
    private static JsonObject winAGame(final GameApiClient player) throws IOException, InterruptedException {
        player.startGame();
        var low = LOWEST_NUMBER;
        var high = HIGHEST_NUMBER;

        while (low <= high) {
            final var midpoint = (low + high) / 2;
            final var state = player.guess(midpoint).asObject();
            if (OUTCOME_CORRECT.equals(state.getString("lastOutcome"))) {
                return state;
            }
            if (OUTCOME_TOO_LOW.equals(state.getString("lastOutcome"))) {
                low = midpoint + 1;
            } else {
                high = midpoint - 1;
            }
        }
        return fail("a binary search over 1 to 10 should always find the number");
    }

    /// Plays a round to a loss by repeating a guess the feedback already showed to be wrong,
    /// and returns the losing state.
    private static JsonObject loseAGame(final GameApiClient player) throws IOException, InterruptedException {
        for (var attempt = 0; attempt < LOSS_ATTEMPTS; attempt++) {
            player.startGame();
            var state = player.guess(LOWEST_NUMBER).asObject();
            if (OUTCOME_CORRECT.equals(state.getString("lastOutcome"))) {
                continue;
            }
            while (STATUS_IN_PROGRESS.equals(state.getString("status"))) {
                state = player.guess(LOWEST_NUMBER).asObject();
            }
            return state;
        }
        return fail("every attempt to lose on purpose was spoiled by a lucky first guess");
    }
}
