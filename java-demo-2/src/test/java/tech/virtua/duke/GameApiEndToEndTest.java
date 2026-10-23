package tech.virtua.duke;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.virtua.duke.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.GameStatus.ACTIVE;
import static tech.virtua.duke.GameStatus.LOST;
import static tech.virtua.duke.GameStatus.WON;
import static tech.virtua.duke.GuessFeedback.REJECTED;
import static tech.virtua.duke.GuessFeedback.TOO_HIGH;
import static tech.virtua.duke.GuessFeedback.TOO_LOW;
import static tech.virtua.duke.HttpStatus.METHOD_NOT_ALLOWED;
import static tech.virtua.duke.HttpStatus.NOT_FOUND;
import static tech.virtua.duke.HttpStatus.OK;
import static tech.virtua.duke.HttpStatus.PAYLOAD_TOO_LARGE;

/// Plays whole games over real HTTP against a server with a known secret.
class GameApiEndToEndTest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int SECRET = 7;
    private static final String WRONG_GUESS = "1";

    private GameServer server;
    private ApiClient player;

    @BeforeEach
    void startServer() throws IOException {
        server = GameServer.start(EPHEMERAL_PORT, () -> SECRET);
        player = new ApiClient(server.port());
    }

    @AfterEach
    void stopServer() {
        player.close();
        server.close();
    }

    @Test
    void startsAGameOnTheFirstRequest() throws Exception {
        final var response = player.currentGame();

        assertEquals(OK.code(), response.statusCode());
        assertEquals(ACTIVE.name(), response.text("status"));
        assertEquals(0, response.number("guessesMade"));
        assertEquals(GUESS_LIMIT, response.number("guessesRemaining"));
        assertTrue(response.isNull("lastGuess"));
    }

    @Test
    void sendsTheRulesAlongWithTheState() throws Exception {
        final var response = player.currentGame();

        assertEquals(1, response.number("lowestNumber"));
        assertEquals(10, response.number("highestNumber"));
        assertEquals(GUESS_LIMIT, response.number("guessLimit"));
    }

    @Test
    void keepsTheSecretUntilTheGameIsOver() throws Exception {
        assertTrue(player.guess("9").isNull("secretNumber"));
        assertEquals(SECRET, player.guess("7").number("secretNumber"));
    }

    @Test
    void playsAWinningGame() throws Exception {
        assertEquals(TOO_LOW.name(), player.guess("3").text("feedback"));
        assertEquals(TOO_HIGH.name(), player.guess("9").text("feedback"));

        final var win = player.guess("7");

        assertEquals(WON.name(), win.text("status"));
        assertEquals(3, win.number("guessesMade"));
        assertEquals(SECRET, win.number("secretNumber"));
        assertTrue(win.text("message").contains("found it in 3 guesses"));
    }

    @Test
    void playsALosingGame() throws Exception {
        IntStream.range(0, GUESS_LIMIT - 1).forEach(_ -> guessQuietly(WRONG_GUESS));

        final var loss = player.guess(WRONG_GUESS);

        assertEquals(LOST.name(), loss.text("status"));
        assertEquals(GUESS_LIMIT, loss.number("guessesMade"));
        assertEquals(0, loss.number("guessesRemaining"));
        assertEquals(SECRET, loss.number("secretNumber"));
    }

    @Test
    void stopsAcceptingGuessesAfterTheGameIsOver() throws Exception {
        player.guess("7");

        final var afterWin = player.guess(WRONG_GUESS);

        assertEquals(WON.name(), afterWin.text("status"));
        assertEquals(1, afterWin.number("guessesMade"));
    }

    @Test
    void doesNotSpendAGuessOnSomethingOutOfRange() throws Exception {
        final var response = player.guess("42");

        assertEquals(OK.code(), response.statusCode());
        assertEquals(REJECTED.name(), response.text("feedback"));
        assertEquals(0, response.number("guessesMade"));
        assertEquals(GUESS_LIMIT, response.number("guessesRemaining"));
    }

    @Test
    void doesNotSpendAGuessOnAnEmptyBody() throws Exception {
        final var response = player.guess("");

        assertEquals(REJECTED.name(), response.text("feedback"));
        assertEquals(0, response.number("guessesMade"));
    }

    @Test
    void startsOverOnRequest() throws Exception {
        player.guess("9");

        final var fresh = player.newGame();

        assertEquals(ACTIVE.name(), fresh.text("status"));
        assertEquals(0, fresh.number("guessesMade"));
        assertTrue(fresh.isNull("lastGuess"));
        assertTrue(fresh.isNull("secretNumber"));
    }

    @Test
    void canStartOverAfterLosing() throws Exception {
        IntStream.range(0, GUESS_LIMIT).forEach(_ -> guessQuietly(WRONG_GUESS));
        assertEquals(LOST.name(), player.currentGame().text("status"));

        player.newGame();

        assertEquals(WON.name(), player.guess("7").text("status"));
    }

    @Test
    void remembersTheGameBetweenRequests() throws Exception {
        player.guess("9");

        assertEquals(1, player.currentGame().number("guessesMade"));
    }

    @Test
    void keepsTwoPlayersApart() throws Exception {
        try (final var otherPlayer = new ApiClient(server.port())) {
            player.guess("9");

            assertEquals(1, player.currentGame().number("guessesMade"));
            assertEquals(0, otherPlayer.currentGame().number("guessesMade"));
        }
    }

    @Test
    void refusesTheWrongMethodOnAKnownPath() throws Exception {
        final var wrongMethodOnRead = player.post(GameRoute.READ.path());
        final var wrongMethodOnGuess = player.get(GameRoute.GUESS.path());

        assertEquals(METHOD_NOT_ALLOWED.code(), wrongMethodOnRead.statusCode());
        assertEquals("GET", wrongMethodOnRead.allow());
        assertEquals(METHOD_NOT_ALLOWED.code(), wrongMethodOnGuess.statusCode());
        assertEquals("POST", wrongMethodOnGuess.allow());
    }

    @Test
    void answersNotFoundForAnUnknownApiPath() throws Exception {
        final var response = player.get("/api/scoreboard");

        assertEquals(NOT_FOUND.code(), response.statusCode());
        assertFalse(response.text("message").isBlank());
    }

    @Test
    void winsOnTheVeryLastGuess() throws Exception {
        IntStream.range(0, GUESS_LIMIT - 1).forEach(_ -> guessQuietly(WRONG_GUESS));
        assertEquals(1, player.currentGame().number("guessesRemaining"));

        final var win = player.guess("7");

        assertEquals(WON.name(), win.text("status"));
        assertEquals(GUESS_LIMIT, win.number("guessesMade"));
        assertEquals(0, win.number("guessesRemaining"));
    }

    @Test
    void canStartOverAfterWinning() throws Exception {
        player.guess("7");
        assertEquals(WON.name(), player.currentGame().text("status"));

        final var fresh = player.newGame();

        assertEquals(ACTIVE.name(), fresh.text("status"));
        assertEquals(0, fresh.number("guessesMade"));
        assertEquals(WON.name(), player.guess("7").text("status"));
    }

    @Test
    void doesNotStartAGameForAnAddressItDoesNotServe() throws Exception {
        assertEquals(NOT_FOUND.code(), player.get("/api/scoreboard").statusCode());

        // A new browser still gets a slot, so the unknown address claimed none.
        assertEquals(0, player.currentGame().number("guessesMade"));
    }

    @Test
    void refusesABodyTooLongToBeAGuess() throws Exception {
        final var response = player.guess("7".repeat(GameApiHandler.MAX_GUESS_BODY_BYTES + 1));

        assertEquals(PAYLOAD_TOO_LARGE.code(), response.statusCode());
        assertEquals(0, player.currentGame().number("guessesMade"));
    }

    private void guessQuietly(final String rawGuess) {
        try {
            player.guess(rawGuess);
        } catch (final IOException networkFailure) {
            throw new AssertionError("Guess could not be sent.", networkFailure);
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while guessing.", interrupted);
        }
    }
}
