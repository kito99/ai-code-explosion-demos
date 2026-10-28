package com.example.guessgame;

/**
 * Thrown when a game session ID is not found.
 */
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String sessionId) {
        super(String.format("Game session not found: %s", sessionId));
    }
}
