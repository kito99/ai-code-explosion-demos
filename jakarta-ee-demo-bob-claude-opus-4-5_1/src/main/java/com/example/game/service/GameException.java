package com.example.game.service;

/**
 * Thrown when a game operation cannot proceed due to an invalid session or
 * game state.
 */
public class GameException extends RuntimeException {
    public GameException(String message) {
        super(message);
    }
}
