package com.example.game;

import com.example.game.model.GameStatus;
import com.example.game.model.GuessFeedback;
import com.example.game.service.GameException;
import com.example.game.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GameService}.
 * No container required — the service has no external dependencies.
 */
class GameServiceTest {

    private GameService service;

    @BeforeEach
    void setUp() {
        service = new GameService();
    }

    @Test
    void newGame_initialStateIsActive() {
        var state = service.newGame();
        assertEquals(GameStatus.ACTIVE, state.status());
        assertEquals(0, state.guessCount());
        assertEquals(GameService.MAX_GUESSES, state.guessesRemaining());
        assertNull(state.lastFeedback());
        assertNull(state.secretNumber());
        assertNotNull(state.sessionId());
    }

    @Test
    void guess_tooLow_feedbackIsTooLow() {
        var sessionId = newGameAndForceSecret(10);
        var state = service.guess(sessionId, 5);
        assertEquals(GuessFeedback.TOO_LOW, state.lastFeedback());
        assertEquals(GameStatus.ACTIVE, state.status());
        assertEquals(1, state.guessCount());
        assertEquals(GameService.MAX_GUESSES - 1, state.guessesRemaining());
    }

    @Test
    void guess_tooHigh_feedbackIsTooHigh() {
        var sessionId = newGameAndForceSecret(1);
        var state = service.guess(sessionId, 5);
        assertEquals(GuessFeedback.TOO_HIGH, state.lastFeedback());
        assertEquals(GameStatus.ACTIVE, state.status());
    }

    @Test
    void guess_correct_statusIsWon() {
        var sessionId = newGameAndForceSecret(7);
        var state = service.guess(sessionId, 7);
        assertEquals(GuessFeedback.CORRECT, state.lastFeedback());
        assertEquals(GameStatus.WON, state.status());
        assertNull(state.secretNumber()); // secret not revealed on win
    }

    @Test
    void guess_maxWrongGuesses_statusIsLost() {
        var sessionId = newGameAndForceSecret(10);
        // Submit MAX_GUESSES - 1 wrong guesses
        for (int i = 0; i < GameService.MAX_GUESSES - 1; i++) {
            service.guess(sessionId, 1);
        }
        var lastState = service.guess(sessionId, 1); // 5th wrong guess
        assertEquals(GameStatus.LOST, lastState.status());
        assertEquals(Integer.valueOf(10), lastState.secretNumber());
        assertEquals(0, lastState.guessesRemaining());
    }

    @Test
    void guess_afterWin_throwsGameException() {
        var sessionId = newGameAndForceSecret(7);
        service.guess(sessionId, 7); // wins
        assertThrows(GameException.class, () -> service.guess(sessionId, 7));
    }

    @Test
    void guess_unknownSession_throwsGameException() {
        assertThrows(GameException.class, () -> service.guess("no-such-id", 5));
    }

    @Test
    void guessesRemaining_neverDropsBelowZero() {
        var sessionId = newGameAndForceSecret(10);
        for (int i = 0; i < GameService.MAX_GUESSES; i++) {
            var state = service.guess(sessionId, 1);
            assertTrue(state.guessesRemaining() >= 0);
        }
    }

    // --- Helpers ---

    /**
     * Creates a game and replaces the session's secret via reflection so we can
     * write deterministic tests without depending on randomness.
     */
    private String newGameAndForceSecret(int secret) {
        var state = service.newGame();
        var sessionId = state.sessionId();
        try {
            var sessionsField = GameService.class.getDeclaredField("sessions");
            sessionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var sessions = (java.util.Map<String, Object>) sessionsField.get(service);
            var session = sessions.get(sessionId);

            var secretField = session.getClass().getDeclaredField("secretNumber");
            secretField.setAccessible(true);
            secretField.set(session, secret);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not force secret number", e);
        }
        return sessionId;
    }
}
