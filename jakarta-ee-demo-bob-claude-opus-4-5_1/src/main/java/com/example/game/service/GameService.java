package com.example.game.service;

import com.example.game.model.GameState;
import com.example.game.model.GameStatus;
import com.example.game.model.GuessFeedback;
import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory game sessions. Each session is identified by a UUID and
 * holds the secret number plus the running guess count.
 *
 * Concurrent sessions are safe because the map is a {@link ConcurrentHashMap}
 * and session-level mutations happen on independent {@link Session} instances.
 */
@ApplicationScoped
public class GameService {

    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 10;
    public static final int MAX_GUESSES = 5;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // --- Public API ---

    /** Creates a new game session and returns the initial state. */
    public GameState newGame() {
        var sessionId = UUID.randomUUID().toString();
        var secret = random.nextInt(MAX_NUMBER - MIN_NUMBER + 1) + MIN_NUMBER;
        sessions.put(sessionId, new Session(secret));
        return stateOf(sessionId, sessions.get(sessionId), null);
    }

    /**
     * Submits a guess for an existing session.
     *
     * @throws GameException if the session does not exist or the game is not active
     */
    public GameState guess(String sessionId, int number) {
        var session = sessions.get(sessionId);
        if (session == null) {
            throw new GameException("Session not found: " + sessionId);
        }
        if (session.status != GameStatus.ACTIVE) {
            throw new GameException("Game is not active for session: " + sessionId);
        }

        session.guessCount++;
        GuessFeedback feedback;
        if (number == session.secretNumber) {
            feedback = GuessFeedback.CORRECT;
            session.status = GameStatus.WON;
        } else if (number < session.secretNumber) {
            feedback = GuessFeedback.TOO_LOW;
        } else {
            feedback = GuessFeedback.TOO_HIGH;
        }

        if (feedback != GuessFeedback.CORRECT && session.guessCount >= MAX_GUESSES) {
            session.status = GameStatus.LOST;
        }

        return stateOf(sessionId, session, feedback);
    }

    // --- Helpers ---

    private GameState stateOf(String sessionId, Session session, GuessFeedback feedback) {
        var secretRevealed = session.status == GameStatus.LOST ? session.secretNumber : null;
        return new GameState(
                sessionId,
                session.status,
                session.guessCount,
                MAX_GUESSES - session.guessCount,
                feedback,
                secretRevealed
        );
    }

    // --- Inner type ---

    private static final class Session {
        final int secretNumber;
        int guessCount = 0;
        GameStatus status = GameStatus.ACTIVE;

        Session(int secretNumber) {
            this.secretNumber = secretNumber;
        }
    }
}
