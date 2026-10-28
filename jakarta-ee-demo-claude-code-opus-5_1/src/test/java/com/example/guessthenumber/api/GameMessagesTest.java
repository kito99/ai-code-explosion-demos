package com.example.guessthenumber.api;

import com.example.guessthenumber.game.Game;
import com.example.guessthenumber.game.GameRules;
import com.example.guessthenumber.game.GuessOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMessagesTest {

    private static final int SECRET = 6;

    private static Game newGame() {
        return new Game(GameRules.STANDARD, SECRET);
    }

    @Test
    void aNewGameSaysHowManyGuessesAreAvailable() {
        assertEquals("Duke is thinking of a number between 1 and 10. You have 5 guesses — good luck!",
                GameMessages.forNewGame(newGame()));
    }

    @Test
    void aLowGuessPointsHigherAndCountsDown() {
        final var game = newGame();
        final var outcome = game.guess(SECRET - 1);

        assertEquals("5 is too low. Try a higher number — you have 4 guesses left.",
                GameMessages.forGuess(SECRET - 1, outcome, game));
    }

    @Test
    void aHighGuessPointsLower() {
        final var game = newGame();
        final var outcome = game.guess(SECRET + 1);

        assertTrue(GameMessages.forGuess(SECRET + 1, outcome, game).startsWith("7 is too high. Try a lower number"));
    }

    @Test
    void theLastRemainingGuessIsSingular() {
        final var game = newGame();
        for (var spent = 0; spent < GameRules.STANDARD.maxGuesses() - 1; spent++) {
            game.guess(SECRET + 1);
        }

        assertTrue(GameMessages.forCurrentState(game).endsWith("You have 1 guess left."),
                GameMessages.forCurrentState(game));
    }

    @Test
    void aWinReportsTheSecretAndTheGuessCount() {
        final var game = newGame();
        final var outcome = game.guess(SECRET);

        assertEquals("You got it! Duke was thinking of 6, and you found it in 1 guess. "
                        + "Add your name to post your score, or start a new game.",
                GameMessages.forGuess(SECRET, outcome, game));
    }

    @Test
    void aLossRevealsTheSecret() {
        final var game = newGame();
        GuessOutcome outcome = null;
        for (var spent = 0; spent < GameRules.STANDARD.maxGuesses(); spent++) {
            outcome = game.guess(SECRET + 1);
        }

        assertEquals("Out of guesses! Duke was thinking of 6. Start a new game to try again.",
                GameMessages.forGuess(SECRET + 1, outcome, game));
    }

    @Test
    void aGameInProgressSaysWhatIsLeft() {
        final var game = newGame();
        game.guess(SECRET + 1);

        assertEquals("Duke is still thinking of a number between 1 and 10. You have 4 guesses left.",
                GameMessages.forCurrentState(game));
    }
}
