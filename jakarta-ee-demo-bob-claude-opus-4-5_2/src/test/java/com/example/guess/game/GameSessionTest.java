package com.example.guess.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameSession}.
 */
class GameSessionTest {

    @Test
    void initialStateIsActive() {
        var session = new GameSession("test-id", 5);
        assertTrue(session.isActive());
        assertFalse(session.isWon());
        assertFalse(session.isLost());
        assertEquals(0, session.guessCount());
        assertEquals(GameSessionStore.MAX_GUESSES, session.guessesRemaining());
    }

    @Test
    void incrementGuessesReducesRemaining() {
        var session = new GameSession("test-id", 5);
        session.incrementGuesses();
        session.incrementGuesses();
        assertEquals(2, session.guessCount());
        assertEquals(GameSessionStore.MAX_GUESSES - 2, session.guessesRemaining());
    }

    @Test
    void markWonSetsWonFlag() {
        var session = new GameSession("test-id", 5);
        session.markWon();
        assertTrue(session.isWon());
        assertFalse(session.isActive());
        assertFalse(session.isLost());
    }

    @Test
    void markLostSetsLostFlag() {
        var session = new GameSession("test-id", 5);
        session.markLost();
        assertTrue(session.isLost());
        assertFalse(session.isActive());
        assertFalse(session.isWon());
    }

    @Test
    void guessesRemainingNeverNegative() {
        var session = new GameSession("test-id", 5);
        for (int i = 0; i <= GameSessionStore.MAX_GUESSES + 2; i++) {
            session.incrementGuesses();
        }
        assertEquals(0, session.guessesRemaining());
    }
}
