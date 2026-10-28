package com.example.guessthenumber.it;

import com.example.guessthenumber.it.GameApiClient.ApiResponse;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Drives the running application over HTTP, against a PostgreSQL container the test
/// provisions itself. The methods run in order because they share one leaderboard.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GuessTheNumberIT {

    private static final String GAME_PATH = "/api/game";
    private static final String GUESSES_PATH = "/api/game/guesses";
    private static final String LEADERBOARD_PATH = "/api/leaderboard";
    private static final int LOWEST = 1;
    private static final int HIGHEST = 10;
    private static final int MAX_GUESSES = 5;
    private static final int TOP_ENTRY_LIMIT = 10;
    private static final int MAX_GAME_ATTEMPTS = 300;
    private static final Duration TELEMETRY_TIMEOUT = Duration.ofSeconds(45);

    private static URI baseUri;

    @BeforeAll
    static void startApplication() throws IOException, InterruptedException {
        baseUri = LibertyTestServer.start();
    }

    @AfterAll
    static void stopApplication() throws IOException, InterruptedException {
        LibertyTestServer.stop();
    }

    @Test
    @Order(10)
    @DisplayName("A fresh database has nothing on the leaderboard")
    void leaderboardStartsEmpty() throws Exception {
        final var response = newPlayer().get(LEADERBOARD_PATH);

        assertEquals(200, response.status());
        assertTrue(response.asArray().isEmpty(), response.body());
    }

    @Test
    @Order(20)
    @DisplayName("A new game starts with every guess available and no secret revealed")
    void startingAGameOffersEveryGuess() throws Exception {
        final var state = startGame(newPlayer());

        assertAll(
                () -> assertEquals("ACTIVE", state.getString("status")),
                () -> assertEquals(0, state.getInt("guessesMade")),
                () -> assertEquals(MAX_GUESSES, state.getInt("guessesRemaining")),
                () -> assertFalse(state.containsKey("secretNumber"), state.toString()),
                () -> assertFalse(state.getBoolean("scorePosted")),
                () -> assertTrue(state.getString("message").contains("between %d and %d".formatted(LOWEST, HIGHEST))));
    }

    @Test
    @Order(30)
    @DisplayName("Reading the game back leaves it untouched")
    void readingTheGameDoesNotChangeIt() throws Exception {
        final var player = newPlayer();
        startGame(player);
        guess(player, LOWEST);

        final var state = player.get(GAME_PATH).asObject();

        assertEquals(1, state.getInt("guessesMade"));
        assertEquals(MAX_GUESSES - 1, state.getInt("guessesRemaining"));
    }

    @Test
    @Order(40)
    @DisplayName("A guess outside the range is refused and costs nothing")
    void aGuessOutsideTheRangeIsRefused() throws Exception {
        final var player = newPlayer();
        startGame(player);

        final var refused = guess(player, HIGHEST + 1);

        assertEquals(400, refused.status());
        assertTrue(refused.message().contains("whole number"), refused.body());
        assertEquals(0, player.get(GAME_PATH).asObject().getInt("guessesMade"));
    }

    @Test
    @Order(50)
    @DisplayName("Guessing before starting a game says there is no game")
    void guessingWithoutAGameIsNotFound() throws Exception {
        final var response = guess(newPlayer(), LOWEST);

        assertEquals(404, response.status());
        assertTrue(response.message().contains("no game"), response.body());
    }

    @Test
    @Order(60)
    @DisplayName("Winning reveals the secret, reports the guess count, and closes the game")
    void winningRevealsTheSecretAndClosesTheGame() throws Exception {
        final var player = newPlayer();
        final var won = playToWin(player);

        assertAll(
                () -> assertEquals("WON", won.getString("status")),
                () -> assertEquals("CORRECT", won.getString("lastGuessOutcome")),
                () -> assertTrue(won.containsKey("secretNumber"), won.toString()),
                () -> assertFalse(won.getBoolean("scorePosted")),
                () -> assertTrue(won.getString("message").startsWith("You got it!"), won.toString()));

        assertEquals(409, guess(player, LOWEST).status());
    }

    @Test
    @Order(70)
    @DisplayName("Posting a win puts the name and guess count on the leaderboard")
    void postingAWinAddsALeaderboardEntry() throws Exception {
        final var player = newPlayer();
        final var won = playToWin(player);

        final var posted = postScore(player, "  Ada  ");

        assertEquals(201, posted.status());
        assertTrue(posted.message().contains("Ada"), posted.body());

        final var entry = findEntry(newPlayer().get(LEADERBOARD_PATH).asArray(), "Ada");
        assertEquals(won.getInt("guessesMade"), entry.getInt("guessCount"));
    }

    @Test
    @Order(80)
    @DisplayName("The same win cannot be posted twice")
    void postingTheSameWinTwiceIsRefused() throws Exception {
        final var player = newPlayer();
        playToWin(player);
        assertEquals(201, postScore(player, "Grace").status());

        final var repeat = postScore(player, "Grace");

        assertEquals(409, repeat.status());
        assertTrue(repeat.message().contains("already on the leaderboard"), repeat.body());
    }

    @Test
    @Order(90)
    @DisplayName("A score cannot be posted without a win behind it")
    void postingWithoutAWinIsRefused() throws Exception {
        final var player = newPlayer();
        startGame(player);

        final var refused = postScore(player, "Alan");

        assertEquals(409, refused.status());
        assertTrue(refused.message().contains("no win to post"), refused.body());
    }

    @Test
    @Order(100)
    @DisplayName("A blank or overlong name records nothing until a valid one arrives")
    void blankAndOverlongNamesAreRefused() throws Exception {
        final var player = newPlayer();
        playToWin(player);

        assertEquals(400, postScore(player, "   ").status());
        assertEquals(400, postScore(player, "D".repeat(31)).status());
        assertEquals(201, postScore(player, "D".repeat(30)).status());
    }

    @Test
    @Order(110)
    @DisplayName("Losing reveals the secret and records nothing")
    void losingRecordsNothing() throws Exception {
        final var entriesBefore = newPlayer().get(LEADERBOARD_PATH).asArray().size();
        final var player = newPlayer();

        final var lost = playToLose(player);

        assertAll(
                () -> assertEquals("LOST", lost.getString("status")),
                () -> assertEquals(MAX_GUESSES, lost.getInt("guessesMade")),
                () -> assertEquals(0, lost.getInt("guessesRemaining")),
                () -> assertTrue(lost.containsKey("secretNumber"), lost.toString()),
                () -> assertTrue(lost.getString("message").startsWith("Out of guesses!"), lost.toString()));

        assertEquals(409, postScore(player, "Should not count").status());
        assertEquals(entriesBefore, newPlayer().get(LEADERBOARD_PATH).asArray().size());
    }

    @Test
    @Order(120)
    @DisplayName("The leaderboard ranks by fewest guesses and breaks ties by what came first")
    void theLeaderboardOrdersByFewestGuessesThenOldest() throws Exception {
        postWinTaking(4, "Slow first");
        postWinTaking(2, "Quick");
        postWinTaking(4, "Slow second");

        final var entries = newPlayer().get(LEADERBOARD_PATH).asArray();

        assertTrue(rankOf(entries, "Quick") < rankOf(entries, "Slow first"), entries.toString());
        assertTrue(rankOf(entries, "Slow first") < rankOf(entries, "Slow second"), entries.toString());
    }

    @Test
    @Order(130)
    @DisplayName("The leaderboard shows at most ten entries, ranked from one")
    void theLeaderboardShowsAtMostTenEntries() throws Exception {
        while (newPlayer().get(LEADERBOARD_PATH).asArray().size() < TOP_ENTRY_LIMIT) {
            final var player = newPlayer();
            playToWin(player);
            assertEquals(201, postScore(player, "Filler").status());
        }
        for (var extra = 0; extra < 3; extra++) {
            final var player = newPlayer();
            playToWin(player);
            assertEquals(201, postScore(player, "Extra").status());
        }

        final var entries = newPlayer().get(LEADERBOARD_PATH).asArray();

        assertEquals(TOP_ENTRY_LIMIT, entries.size());
        assertEquals(1, entries.getJsonObject(0).getInt("rank"));
        assertEquals(TOP_ENTRY_LIMIT, entries.getJsonObject(TOP_ENTRY_LIMIT - 1).getInt("rank"));
        assertTrue(isOrderedByFewestGuesses(entries), entries.toString());
    }

    @Test
    @Order(140)
    @DisplayName("The running app serves an OpenAPI document and browsable documentation")
    void theOpenApiDocumentDescribesTheApi() throws Exception {
        final var document = newPlayer().get("/openapi");
        final var ui = newPlayer().get("/openapi/ui");

        assertEquals(200, document.status());
        assertAll(
                () -> assertTrue(document.body().contains("/game/guesses"), document.body()),
                () -> assertTrue(document.body().contains("/leaderboard"), document.body()),
                () -> assertTrue(document.body().contains("Guess the Number with Duke"), document.body()),
                () -> assertEquals(200, ui.status()));
    }

    @Test
    @Order(150)
    @DisplayName("Playing produces traces and metrics in the exported telemetry")
    void telemetryReachesTheConsoleLog() throws Exception {
        final var player = newPlayer();
        playToWin(player);

        assertAll(
                () -> assertTrue(awaitConsoleLog("game.start"), "no game.start span was exported"),
                () -> assertTrue(awaitConsoleLog("game.guess"), "no game.guess span was exported"),
                () -> assertTrue(awaitConsoleLog("game.started"), "no game.started metric was exported"),
                () -> assertTrue(awaitConsoleLog("game.won"), "no game.won metric was exported"));
    }

    @Test
    @Order(160)
    @DisplayName("Leaderboard entries are still there after the app restarts")
    void leaderboardEntriesSurviveARestart() throws Exception {
        final var before = newPlayer().get(LEADERBOARD_PATH).asArray();

        LibertyTestServer.restart();

        final var after = newPlayer().get(LEADERBOARD_PATH).asArray();
        assertEquals(before, after);
    }

    private static GameApiClient newPlayer() {
        return new GameApiClient(baseUri);
    }

    private static JsonObject startGame(final GameApiClient player) throws Exception {
        final var response = player.post(GAME_PATH);
        assertEquals(200, response.status(), response.body());
        return response.asObject();
    }

    private static ApiResponse guess(final GameApiClient player, final int guess) throws Exception {
        return player.post(GUESSES_PATH, Json.createObjectBuilder().add("guess", guess).build());
    }

    private static ApiResponse postScore(final GameApiClient player, final String playerName) throws Exception {
        return player.post(LEADERBOARD_PATH, Json.createObjectBuilder().add("playerName", playerName).build());
    }

    /// Narrows the range with the high and low feedback, which always lands the secret
    /// inside the guess limit.
    private static JsonObject playToWin(final GameApiClient player) throws Exception {
        startGame(player);
        var low = LOWEST;
        var high = HIGHEST;
        while (true) {
            final var attempt = (low + high) / 2;
            final var response = guess(player, attempt);
            assertEquals(200, response.status(), response.body());
            final var state = response.asObject();
            final var outcome = state.getString("lastGuessOutcome");
            if ("CORRECT".equals(outcome)) {
                return state;
            }
            if ("TOO_LOW".equals(outcome)) {
                low = attempt + 1;
            } else {
                high = attempt - 1;
            }
        }
    }

    /// Spends every guess on the lowest number, which is wrong unless the secret happens to
    /// be that number, in which case the game is restarted.
    private static JsonObject playToLose(final GameApiClient player) throws Exception {
        for (var attempt = 0; attempt < MAX_GAME_ATTEMPTS; attempt++) {
            startGame(player);
            JsonObject state = null;
            for (var spent = 0; spent < MAX_GUESSES; spent++) {
                final var response = guess(player, LOWEST);
                assertEquals(200, response.status(), response.body());
                state = response.asObject();
                if ("CORRECT".equals(state.getString("lastGuessOutcome", ""))) {
                    state = null;
                    break;
                }
            }
            if (state != null) {
                return state;
            }
        }
        throw new IllegalStateException("Could not reach a losing game in %d attempts".formatted(MAX_GAME_ATTEMPTS));
    }

    /// Replays games until one is won in exactly the wanted number of guesses, then posts it.
    private static void postWinTaking(final int guessCount, final String playerName) throws Exception {
        for (var attempt = 0; attempt < MAX_GAME_ATTEMPTS; attempt++) {
            final var player = newPlayer();
            if (playToWin(player).getInt("guessesMade") == guessCount) {
                assertEquals(201, postScore(player, playerName).status());
                return;
            }
        }
        throw new IllegalStateException("Could not win in exactly %d guesses".formatted(guessCount));
    }

    private static JsonObject findEntry(final JsonArray entries, final String playerName) {
        return entries.getValuesAs(JsonObject.class).stream()
                .filter(entry -> playerName.equals(entry.getString("playerName")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No entry for %s in %s".formatted(playerName, entries)));
    }

    private static int rankOf(final JsonArray entries, final String playerName) {
        return findEntry(entries, playerName).getInt("rank");
    }

    private static boolean isOrderedByFewestGuesses(final JsonArray entries) {
        final var counts = entries.getValuesAs(JsonObject.class).stream()
                .map(entry -> entry.getInt("guessCount"))
                .toList();
        return counts.equals(counts.stream().sorted().toList());
    }

    private static boolean awaitConsoleLog(final String expected) throws IOException, InterruptedException {
        final var deadline = System.nanoTime() + TELEMETRY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (LibertyTestServer.consoleLog().contains(expected)) {
                return true;
            }
            Thread.sleep(Duration.ofSeconds(1).toMillis());
        }
        return false;
    }
}
