package com.example.duke.model;

/**
 * Represents the current state of a single game session.
 * Games are stateless on the server — the client sends state back on each request.
 */
public record GameState(
        int secretNumber,
        int guessesUsed,
        int guessesRemaining,
        GameStatus status,
        String message,
        boolean won
) {
    public static final int MAX_GUESSES = 5;
    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 10;
}
