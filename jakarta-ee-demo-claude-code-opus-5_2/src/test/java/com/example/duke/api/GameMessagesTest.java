package com.example.duke.api;

import com.example.duke.game.Game;
import com.example.duke.game.GameSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.example.duke.game.GameRules.MAX_GUESSES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The wording is part of what the requirements ask for, so it is pinned down here: every
/// message says what just happened and what the player can do next.
class GameMessagesTest {

    private static final int SECRET = 7;

    @Test
    @DisplayName("a fresh game explains the range and the guess limit")
    void describesAFreshGame() {
        assertEquals("Duke is thinking of a number from 1 to 10. You have 5 guesses — good luck!",
                GameMessages.forGame(newGame()));
    }

    @Test
    @DisplayName("a low guess asks for a higher number and counts what is left")
    void describesALowGuess() {
        assertEquals("Too low. Try a higher number — 4 guesses left.",
                GameMessages.forGame(after(SECRET - 1)));
    }

    @Test
    @DisplayName("a high guess asks for a lower number")
    void describesAHighGuess() {
        assertEquals("Too high. Try a lower number — 4 guesses left.",
                GameMessages.forGame(after(SECRET + 1)));
    }

    @Test
    @DisplayName("the last guess before the limit is counted in the singular")
    void countsOneRemainingGuessInTheSingular() {
        final var tooLowEveryTime = new int[MAX_GUESSES - 1];
        Arrays.fill(tooLowEveryTime, SECRET - 1);

        assertEquals("Too low. Try a higher number — 1 guess left.",
                GameMessages.forGame(after(tooLowEveryTime)));
    }

    @Test
    @DisplayName("a win reports the guesses it took and offers the leaderboard")
    void describesAWin() {
        assertEquals("You got it in 1 guess! Enter a name to post your score, or play again.",
                GameMessages.forGame(after(SECRET)));
    }

    @Test
    @DisplayName("a loss reveals the secret number and offers another round")
    void describesALoss() {
        final var tooHighEveryTime = new int[MAX_GUESSES];
        Arrays.fill(tooHighEveryTime, SECRET + 1);

        assertEquals("Out of guesses. Duke was thinking of 7. Play again to try your luck.",
                GameMessages.forGame(after(tooHighEveryTime)));
    }

    @Test
    @DisplayName("a posted score is confirmed by name")
    void confirmsAPostedScore() {
        assertEquals("Thanks, Duke! Your win in 3 guesses is on the leaderboard. Play again, or go see where you landed.",
                GameMessages.scorePosted("Duke", 3));
    }

    @Test
    @DisplayName("every message says what the player can do next")
    void alwaysOffersANextStep() {
        assertTrue(GameMessages.started().contains("good luck"));
        assertTrue(GameMessages.won(2).contains("play again"));
        assertTrue(GameMessages.lost(SECRET).contains("Play again"));
    }

    private static GameSnapshot newGame() {
        return snapshotOf(new Game(SECRET));
    }

    /// Plays `guesses` in order and hands back the round as it stands afterwards.
    private static GameSnapshot after(final int... guesses) {
        final var game = new Game(SECRET);
        for (final var guess : guesses) {
            game.recordGuess(guess);
        }
        return snapshotOf(game);
    }

    private static GameSnapshot snapshotOf(final Game game) {
        return GameSnapshot.of(game, game.winningGuessCount().isPresent());
    }
}
