package com.example.duke.game;

/**
 * Immutable snapshot of game state returned to the API layer.
 * The secret number is intentionally absent — it must never be serialised to a response.
 */
public record Game(
        String gameId,
        int guessesUsed,
        int guessesRemaining,
        GameStatus status,
        String lastFeedback
) {

    public static final int MAX_GUESSES = 5;
    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 10;
}
