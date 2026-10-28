package com.example.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameTest {

    private static final int SECRET = 6;

    @Test
    void givesHighAndLowFeedback() {
        final var game = new Game("id", SECRET);
        assertEquals(GuessResult.TOO_LOW, game.guess(2));
        assertEquals(GuessResult.TOO_HIGH, game.guess(9));
        assertEquals(2, game.guessesMade());
        assertEquals(3, game.guessesRemaining());
    }

    @Test
    void winningStopsFurtherGuesses() {
        final var game = new Game("id", SECRET);
        assertEquals(GuessResult.CORRECT, game.guess(SECRET));
        assertEquals(GameStatus.WON, game.status());
        assertThrows(GameStateException.class, () -> game.guess(1));
    }

    @Test
    void losingAfterFiveGuessesRevealsSecret() {
        final var game = new Game("id", SECRET);
        assertTrue(game.revealedSecret().isEmpty());
        for (var attempt = 0; attempt < GameRules.MAX_GUESSES; attempt++) {
            game.guess(1);
        }
        assertEquals(GameStatus.LOST, game.status());
        assertEquals(SECRET, game.revealedSecret().orElseThrow());
        assertEquals(0, game.guessesRemaining());
        assertThrows(GameStateException.class, () -> game.guess(SECRET));
    }

    @Test
    void lastGuessCanStillWin() {
        final var game = new Game("id", SECRET);
        for (var attempt = 0; attempt < GameRules.MAX_GUESSES - 1; attempt++) {
            game.guess(1);
        }
        assertEquals(GuessResult.CORRECT, game.guess(SECRET));
        assertEquals(GameStatus.WON, game.status());
    }

    @Test
    void outOfRangeGuessDoesNotCount() {
        final var game = new Game("id", SECRET);
        assertThrows(InvalidGuessException.class, () -> game.guess(0));
        assertThrows(InvalidGuessException.class, () -> game.guess(11));
        assertEquals(0, game.guessesMade());
    }

    @Test
    void scoreCanBeClaimedOnlyOnceAfterAWin() {
        final var game = new Game("id", SECRET);
        assertThrows(GameStateException.class, game::claimScore);
        game.guess(1);
        game.guess(SECRET);
        assertEquals(2, game.claimScore());
        assertThrows(GameStateException.class, game::claimScore);
        game.releaseScore();
        assertEquals(2, game.claimScore());
    }
}
