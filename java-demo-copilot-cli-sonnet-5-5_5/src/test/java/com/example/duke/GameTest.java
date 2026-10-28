package com.example.duke;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GameTest {
    @Test
    void secretIsAlwaysInRangeAndEveryValueOccurs() {
        var seen = new HashSet<Integer>();
        var random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            int secret = new Game(random).secret();
            assertTrue(secret >= 1 && secret <= 10);
            seen.add(secret);
        }
        assertEquals(10, seen.size());
    }

    @Test
    void losesAfterFiveWrongGuessesAndRemainingNeverGoesNegative() {
        var game = new Game(new Random(1));
        int wrong = game.secret() == 1 ? 2 : 1;
        for (int i = 0; i < 5; i++) {
            game.guess(wrong);
        }
        assertEquals(Game.State.LOST, game.state());
        assertEquals(0, game.guessesRemaining());
        assertThrows(IllegalStateException.class, () -> game.guess(wrong));
    }

    @Test
    void correctGuessWinsWithGuessCount() {
        var game = new Game(new Random(3));
        assertEquals(Game.Outcome.CORRECT, game.guess(game.secret()));
        assertEquals(Game.State.WON, game.state());
        assertEquals(1, game.guessesMade());
    }

    @Test
    void nameValidation() {
        assertNull(NameValidator.normalize("   "));
        assertNull(NameValidator.normalize(null));
        assertNull(NameValidator.normalize("x".repeat(31)));
        assertEquals("x".repeat(30), NameValidator.normalize(" " + "x".repeat(30) + " "));
    }
}
