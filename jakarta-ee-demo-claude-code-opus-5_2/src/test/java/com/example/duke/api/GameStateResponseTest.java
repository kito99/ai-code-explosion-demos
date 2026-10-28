package com.example.duke.api;

import com.example.duke.api.dto.GameStateResponse;
import com.example.duke.game.Game;
import com.example.duke.game.GameSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.duke.game.GameRules.MAX_GUESSES;
import static com.example.duke.game.GameStatus.IN_PROGRESS;
import static com.example.duke.game.GameStatus.WON;
import static com.example.duke.game.GuessOutcome.TOO_LOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What reaches the screen. A field that does not apply yet has to arrive as null, because
/// JSON Binding leaves those out of the document entirely.
class GameStateResponseTest {

    private static final int SECRET = 7;

    @Test
    @DisplayName("a fresh game sends no last guess and no secret number")
    void leavesOutWhatHasNotHappenedYet() {
        final var response = GameStateResponse.from(GameSnapshot.of(new Game(SECRET), false));

        assertEquals(IN_PROGRESS, response.status());
        assertEquals(0, response.guessesMade());
        assertEquals(MAX_GUESSES, response.guessesRemaining());
        assertNull(response.lastGuess(), "there is no last guess yet");
        assertNull(response.lastOutcome(), "there is no outcome yet");
        assertNull(response.secretNumber(), "the secret number stays hidden while the game is on");
        assertTrue(response.message().contains("thinking of a number"));
    }

    @Test
    @DisplayName("an unfinished game reports its last guess but still hides the number")
    void reportsTheLastGuessWithoutGivingTheAnswerAway() {
        final var game = new Game(SECRET);
        game.recordGuess(SECRET - 1);

        final var response = GameStateResponse.from(GameSnapshot.of(game, false));

        assertEquals(SECRET - 1, response.lastGuess());
        assertEquals(TOO_LOW, response.lastOutcome());
        assertNull(response.secretNumber());
    }

    @Test
    @DisplayName("a win reveals the number and offers the leaderboard")
    void revealsTheNumberOnAWin() {
        final var game = new Game(SECRET);
        game.recordGuess(SECRET);

        final var response = GameStateResponse.from(GameSnapshot.of(game, true));

        assertEquals(WON, response.status());
        assertEquals(SECRET, response.secretNumber());
        assertTrue(response.canPostScore());
    }
}
