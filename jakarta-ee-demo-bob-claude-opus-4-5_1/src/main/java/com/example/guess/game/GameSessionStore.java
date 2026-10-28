package com.example.guess.game;

import com.example.guess.model.GuessResult;
import com.example.guess.model.GuessResult.Feedback;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory game sessions. Sessions are keyed by a UUID generated at
 * game start and supplied by the client on every subsequent request.
 *
 * Sessions are intentionally ephemeral: they disappear when the server
 * restarts, consistent with the BRD assumption that in-progress games do not
 * persist.
 */
@ApplicationScoped
public class GameSessionStore {

    static final int MAX_GUESSES = 5;
    static final int MIN_NUMBER = 1;
    static final int MAX_NUMBER = 10;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    /**
     * Starts a new game and returns its session ID.
     */
    @WithSpan("game.start")
    public String startGame() {
        var id = UUID.randomUUID().toString();
        var secret = random.nextInt(MAX_NUMBER - MIN_NUMBER + 1) + MIN_NUMBER;
        sessions.put(id, new GameSession(id, secret));
        return id;
    }

    /**
     * Submits a guess for an existing session.
     *
     * @return the result of the guess, or {@code null} if the session is unknown
     */
    @WithSpan("game.guess")
    public GuessResult submitGuess(String sessionId, int guess) {
        var session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }

        if (!session.isActive()) {
            return new GuessResult(sessionId, Feedback.GAME_OVER,
                    session.guessCount(), 0, null);
        }

        if (guess < MIN_NUMBER || guess > MAX_NUMBER) {
            return new GuessResult(sessionId, Feedback.INVALID,
                    session.guessCount(), session.guessesRemaining(), null);
        }

        session.incrementGuesses();

        if (guess == session.secretNumber()) {
            session.markWon();
            return new GuessResult(sessionId, Feedback.CORRECT,
                    session.guessCount(), 0, null);
        }

        if (session.guessesRemaining() == 0) {
            session.markLost();
            return new GuessResult(sessionId, Feedback.GAME_OVER,
                    session.guessCount(), 0, session.secretNumber());
        }

        var feedback = guess < session.secretNumber() ? Feedback.TOO_LOW : Feedback.TOO_HIGH;
        return new GuessResult(sessionId, feedback,
                session.guessCount(), session.guessesRemaining(), null);
    }

    /**
     * Returns the session for the given id, or {@code null} if absent.
     */
    public GameSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /** Removes a session to free memory after leaderboard posting. */
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
