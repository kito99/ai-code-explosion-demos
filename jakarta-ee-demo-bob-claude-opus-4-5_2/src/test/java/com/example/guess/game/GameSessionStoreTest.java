package com.example.guess.game;

import com.example.guess.model.GuessResult;
import com.example.guess.model.GuessResult.Feedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameSessionStore} in isolation (no CDI container).
 */
class GameSessionStoreTest {

    private GameSessionStore store;

    @BeforeEach
    void setUp() {
        store = new GameSessionStore();
    }

    @Test
    void startGameReturnsUniqueIds() {
        var id1 = store.startGame();
        var id2 = store.startGame();
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    void newSessionIsActive() {
        var id = store.startGame();
        var session = store.getSession(id);
        assertNotNull(session);
        assertTrue(session.isActive());
        assertFalse(session.isWon());
        assertFalse(session.isLost());
        assertEquals(0, session.guessCount());
        assertEquals(GameSessionStore.MAX_GUESSES, session.guessesRemaining());
    }

    @Test
    void invalidGuessDoesNotCountAgainstLimit() {
        var id = store.startGame();
        var result = store.submitGuess(id, 0);
        assertEquals(Feedback.INVALID, result.feedback());
        assertEquals(0, result.guessCount());
        assertEquals(GameSessionStore.MAX_GUESSES, result.guessesRemaining());
    }

    @Test
    void invalidHighGuessDoesNotCountAgainstLimit() {
        var id = store.startGame();
        var result = store.submitGuess(id, 11);
        assertEquals(Feedback.INVALID, result.feedback());
        assertEquals(0, result.guessCount());
    }

    @Test
    void unknownSessionReturnsNull() {
        assertNull(store.submitGuess("nonexistent", 5));
    }

    @Test
    void exhaustingGuessesEndsInLoss() {
        var id = store.startGame();
        var session = store.getSession(id);
        var secret = session.secretNumber();

        // submit 5 wrong guesses
        var wrongGuess = secret == 1 ? 2 : 1;
        GuessResult last = null;
        for (int i = 0; i < GameSessionStore.MAX_GUESSES; i++) {
            last = store.submitGuess(id, wrongGuess);
        }
        assertNotNull(last);
        assertEquals(Feedback.GAME_OVER, last.feedback());
        assertEquals(secret, last.secretNumber());
        assertFalse(session.isActive());
        assertTrue(session.isLost());
    }

    @Test
    void correctGuessEndsInWin() {
        var id = store.startGame();
        var session = store.getSession(id);
        var secret = session.secretNumber();

        var result = store.submitGuess(id, secret);
        assertEquals(Feedback.CORRECT, result.feedback());
        assertTrue(session.isWon());
        assertFalse(session.isActive());
    }

    @Test
    void feedbackDirectionIsCorrect() {
        var id = store.startGame();
        var session = store.getSession(id);
        var secret = session.secretNumber();

        if (secret > GameSessionStore.MIN_NUMBER) {
            var result = store.submitGuess(id, secret - 1);
            assertEquals(Feedback.TOO_LOW, result.feedback());
        } else {
            // secret == MIN_NUMBER (1): only TOO_HIGH is possible
            var id2 = store.startGame();
            var session2 = store.getSession(id2);
            if (session2.secretNumber() < GameSessionStore.MAX_NUMBER) {
                var result2 = store.submitGuess(id2, session2.secretNumber() + 1);
                assertEquals(Feedback.TOO_HIGH, result2.feedback());
            }
        }
    }

    @Test
    void guessAfterGameOverReturnsGameOver() {
        var id = store.startGame();
        var session = store.getSession(id);
        var secret = session.secretNumber();

        store.submitGuess(id, secret); // win
        var result = store.submitGuess(id, secret);
        assertEquals(Feedback.GAME_OVER, result.feedback());
    }

    @Test
    void removeSessionMakesItUnavailable() {
        var id = store.startGame();
        store.removeSession(id);
        assertNull(store.getSession(id));
    }
}
