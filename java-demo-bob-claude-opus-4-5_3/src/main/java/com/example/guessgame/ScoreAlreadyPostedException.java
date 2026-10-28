package com.example.guessgame;

/**
 * Thrown when a score is posted for a session that has already been recorded.
 */
public class ScoreAlreadyPostedException extends RuntimeException {

    public ScoreAlreadyPostedException(String sessionId) {
        super(String.format("A score has already been posted for session: %s", sessionId));
    }
}
