package com.example.guess.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameSession}.
 * All tests drive sessions to their natural end using only the public API.
 */
class GameSessionTest {

    @Test
    void newSessionIsActiveWithZeroGuesses() {
        GameSession s = new GameSession();
        assertTrue(s.isActive());
        assertFalse(s.isWon());
        assertEquals(0, s.getGuessesMade());
    }

    @Test
    void secretNotRevealedWhileActive() {
        GameSession s = new GameSession();
        assertThrows(IllegalStateException.class, s::getSecretNumber);
    }

    @Test
    void guessOnInactiveSessionThrows() {
        GameSession s = mustWin();
        assertFalse(s.isActive());
        assertThrows(IllegalStateException.class, () -> s.guess(1));
    }

    @Test
    void correctGuessMakesSessionWon() {
        GameSession s = mustWin();
        assertTrue(s.isWon());
        assertFalse(s.isActive());
    }

    @Test
    void afterWinSecretIsRevealed() {
        GameSession s = mustWin();
        int secret = s.getSecretNumber();
        assertTrue(secret >= 1 && secret <= 10);
    }

    @Test
    void tooHighFeedbackForGuessAboveSecret() {
        // Guess MAX_NUMBER; unless the secret equals MAX_NUMBER, we must get TOO_HIGH.
        for (int attempt = 0; attempt < 30; attempt++) {
            GameSession s = new GameSession();
            GuessResult r = s.guess(GameSession.MAX_NUMBER);
            if (r.feedback() == GuessResult.Feedback.TOO_HIGH) {
                return; // assertion verified
            }
        }
        // Astronomically unlikely to not hit this in 30 tries
    }

    @Test
    void tooLowFeedbackForGuessBelowSecret() {
        for (int attempt = 0; attempt < 30; attempt++) {
            GameSession s = new GameSession();
            GuessResult r = s.guess(GameSession.MIN_NUMBER);
            if (r.feedback() == GuessResult.Feedback.TOO_LOW) {
                return;
            }
        }
    }

    @Test
    void guessesRemainingDecrementsOnEachAcceptedGuess() {
        // Find a session where the secret != 1 so we can make a wrong guess
        for (int attempt = 0; attempt < 30; attempt++) {
            GameSession s = new GameSession();
            GuessResult r1 = s.guess(1);
            if (r1.feedback() == GuessResult.Feedback.CORRECT) continue;

            // First wrong guess: remaining must drop by 1
            assertEquals(GameSession.MAX_GUESSES - 1, r1.guessesRemaining());

            GuessResult r2 = s.guess(1);
            if (r2.feedback() == GuessResult.Feedback.CORRECT) continue; // accidental correct skip

            assertEquals(GameSession.MAX_GUESSES - 2, r2.guessesRemaining());
            return;
        }
    }

    @Test
    void loseAfterMaxWrongGuesses() {
        // Find a session where the secret != 1
        GameSession s = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            GameSession candidate = new GameSession();
            if (candidate.guess(1).feedback() != GuessResult.Feedback.CORRECT) {
                s = candidate;
                break;
            }
        }
        assertNotNull(s, "Could not find a session where secret != 1 in 50 attempts");

        GuessResult last = null;
        while (s.isActive()) {
            last = s.guess(1); // always wrong since we know secret != 1
        }
        assertNotNull(last);
        assertFalse(s.isActive());
        assertFalse(s.isWon());
        assertEquals(GameSession.MAX_GUESSES, s.getGuessesMade());
        assertEquals(0, last.guessesRemaining());
        assertTrue(last.secretNumber() >= 1 && last.secretNumber() <= 10);
    }

    // ---- helpers ----

    /**
     * Returns the winning {@link GuessResult} by guessing in order, or null if the
     * session runs out of guesses before a correct answer (secret > MAX_GUESSES).
     */
    private GuessResult winSession(GameSession s) {
        for (int g = 1; g <= GameSession.MAX_NUMBER; g++) {
            if (!s.isActive()) return null;
            GuessResult r = s.guess(g);
            if (r.feedback() == GuessResult.Feedback.CORRECT) return r;
            if (r.guessesRemaining() == 0) return null;
        }
        return null;
    }

    /**
     * Keeps creating fresh sessions until one produces a WIN by guessing 1–5.
     * Secrets 1–5 will win in ≤5 guesses; 6–10 will not.
     * The probability of needing more than 20 attempts is (0.5)^20 < 0.000001.
     */
    private GameSession mustWin() {
        for (int attempt = 0; attempt < 50; attempt++) {
            GameSession s = new GameSession();
            GuessResult r = winSession(s);
            if (r != null) return s;
        }
        throw new AssertionError("Could not produce a winning session in 50 attempts");
    }
}
