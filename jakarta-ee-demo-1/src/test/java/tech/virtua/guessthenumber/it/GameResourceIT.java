package tech.virtua.guessthenumber.it;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plays the game over HTTP against a running Open Liberty server. */
class GameResourceIT {

    private static final int MAX_GUESSES = 5;

    private GameClient player;

    @BeforeEach
    void setUp() {
        player = new GameClient();
    }

    @AfterEach
    void tearDown() {
        player.close();
    }

    @Test
    @DisplayName("A fresh session gets a game with every guess available")
    void firstVisitStartsAGame() {
        GameClient.Reply reply = player.currentGame();

        assertEquals(200, reply.status());
        assertEquals("IN_PROGRESS", reply.gameStatus());
        assertEquals(0, reply.guessesMade());
        assertEquals(MAX_GUESSES, reply.guessesRemaining());
        assertEquals(MAX_GUESSES, reply.body().getInt("maxGuesses"));
        assertEquals(1, reply.body().getInt("minNumber"));
        assertEquals(10, reply.body().getInt("maxNumber"));
        assertFalse(reply.message().isBlank(), "The player always gets something to read");
        assertFalse(reply.body().containsKey("secretNumber"),
                "An in-progress game must not hand back the answer");
    }

    @Test
    @DisplayName("An accepted guess costs one guess and comes back with feedback")
    void acceptedGuessProducesFeedback() {
        GameClient.Reply reply = player.guess(5);

        assertEquals(200, reply.status());
        assertEquals(1, reply.guessesMade());
        assertEquals(MAX_GUESSES - 1, reply.guessesRemaining());
        assertEquals(5, reply.body().getInt("lastGuess"));
        assertTrue(reply.body().getString("lastHint").matches("TOO_HIGH|TOO_LOW|CORRECT"),
                "Unexpected hint: " + reply.body().getString("lastHint"));
    }

    @ParameterizedTest
    @DisplayName("Input that is not a whole number from 1 to 10 never costs a guess")
    @ValueSource(strings = {"0", "11", "-4", "3.5", "seven", "", "   ", "1e1"})
    void invalidInputIsRejected(String input) {
        spendOneGuessWithoutWinning();

        GameClient.Reply reply = player.guess(input);

        assertEquals(400, reply.status());
        assertEquals(1, reply.guessesMade(), "Rejected input must not count: " + input);
        assertEquals(MAX_GUESSES - 1, reply.guessesRemaining());
        assertTrue(reply.message().contains("didn't count"), reply.message());
    }

    @Test
    @DisplayName("Finding the number wins the game and reveals it")
    void playingWellWins() {
        GameClient.Reply reply = playToAWin();

        assertEquals("WON", reply.gameStatus());
        assertTrue(reply.body().containsKey("secretNumber"), "A win reveals the answer");
        assertEquals(reply.body().getInt("lastGuess"), reply.body().getInt("secretNumber"));
        assertTrue(reply.message().contains(reply.guessesMade() + " guess"), reply.message());
    }

    @Test
    @DisplayName("Running out of guesses loses the game and reveals the number")
    void runningOutLoses() {
        JsonObject game = playUntilLoss();

        assertEquals(MAX_GUESSES, game.getInt("guessesMade"));
        assertEquals(0, game.getInt("guessesRemaining"));
        assertTrue(game.containsKey("secretNumber"), "A loss reveals the answer");
        assertTrue(game.getString("message").contains("Out of guesses"), game.getString("message"));
    }

    @Test
    @DisplayName("A finished game takes no more guesses")
    void finishedGameRefusesGuesses() {
        JsonObject finished = playUntilLoss();
        int guessesWhenFinished = finished.getInt("guessesMade");

        GameClient.Reply reply = player.guess(4);

        assertEquals(409, reply.status());
        assertEquals(guessesWhenFinished, reply.guessesMade());
    }

    @Test
    @DisplayName("Play again clears the board and deals a new number")
    void playAgainResetsTheGame() {
        player.guess(2);
        player.guess(9);

        GameClient.Reply reply = player.newGame();

        assertEquals(200, reply.status());
        assertEquals("IN_PROGRESS", reply.gameStatus());
        assertEquals(0, reply.guessesMade());
        assertEquals(MAX_GUESSES, reply.guessesRemaining());
        assertFalse(reply.body().containsKey("lastGuess"), "A new game clears the old feedback");
        assertFalse(reply.body().containsKey("lastHint"));
        assertFalse(reply.body().containsKey("secretNumber"));
    }

    @Test
    @DisplayName("Play again works straight from a finished game")
    void playAgainWorksAfterTheGameEnds() {
        playUntilLoss();

        assertEquals("IN_PROGRESS", player.newGame().gameStatus());
        assertEquals(200, player.guess(5).status(), "The new game should take guesses again");
    }

    @Test
    @DisplayName("Two players get their own games")
    void sessionsAreIndependent() {
        try (GameClient other = new GameClient()) {
            player.guess(1);
            player.guess(2);
            other.guess(3);

            assertEquals(2, player.currentGame().guessesMade());
            assertEquals(1, other.currentGame().guessesMade());
        }
    }

    @Test
    @DisplayName("A body the server cannot read is answered, not blown up")
    void malformedBodyGetsAFriendlyAnswer() {
        HttpResponse<String> response = player.postRawGuessBody("{\"guess\": { \"nope\": true } }");

        assertEquals(400, response.statusCode(), "A body the binder rejects is the caller's fault");
        assertEquals(MAX_GUESSES, player.currentGame().guessesRemaining(),
                "An unreadable body must not cost the player a guess");
    }

    @Test
    @DisplayName("The game page is served with Duke on it")
    void frontEndIsServed() {
        HttpResponse<String> page = player.homePage();

        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Guess the Number"), "Missing the game heading");
        assertTrue(page.body().contains("duke"), "Duke should be on the page");
    }

    /** Narrows in on the secret number using the server's high/low feedback. */
    private GameClient.Reply playToAWin() {
        int low = 1;
        int high = 10;

        for (int attempt = 0; attempt < MAX_GUESSES; attempt++) {
            int guess = (low + high) / 2;
            GameClient.Reply reply = player.guess(guess);
            assertEquals(200, reply.status());

            switch (reply.body().getString("lastHint")) {
                case "CORRECT" -> {
                    return reply;
                }
                case "TOO_HIGH" -> high = guess - 1;
                case "TOO_LOW" -> low = guess + 1;
                default -> throw new AssertionError("Unexpected hint in " + reply.body());
            }
        }
        throw new AssertionError("Binary search should find a number from 1 to 10 within "
                + MAX_GUESSES + " guesses");
    }

    /**
     * Leaves the player one guess into a game that is still running. A guess can always
     * turn out to be the answer, so this deals again until one does not.
     */
    private void spendOneGuessWithoutWinning() {
        for (int deal = 0; deal < 20; deal++) {
            player.newGame();

            GameClient.Reply reply = player.guess(1);
            assertEquals(200, reply.status());
            if (!"CORRECT".equals(reply.body().getString("lastHint"))) {
                return;
            }
        }
        throw new AssertionError("Twenty deals in a row started with the answer, which cannot happen");
    }

    /**
     * Loses a game on purpose by spending every guess on one number known to be wrong.
     * If the probe guess happens to be the answer, this deals a new game and tries again.
     */
    private JsonObject playUntilLoss() {
        for (int deal = 0; deal < 20; deal++) {
            player.newGame();

            JsonObject game = player.guess(1).body();
            if ("CORRECT".equals(game.getString("lastHint"))) {
                continue;
            }
            while ("IN_PROGRESS".equals(game.getString("status"))) {
                game = player.guess(1).body();
            }

            assertEquals("LOST", game.getString("status"));
            return game;
        }
        throw new AssertionError("Twenty deals in a row started with the answer, which cannot happen");
    }
}
