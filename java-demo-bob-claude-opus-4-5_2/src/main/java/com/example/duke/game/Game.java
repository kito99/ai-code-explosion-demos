package com.example.duke.game;

/**
 * Immutable snapshot of game state returned to the API layer.
 * The secret number is included only when status is LOST; it is null otherwise.
 * The secret number must never appear in a response while the game is still ACTIVE or WON.
 */
public record Game(
        String gameId,
        int guessesUsed,
        int guessesRemaining,
        GameStatus status,
        String lastFeedback,
        Integer revealedNumber
) {

    public static final int MAX_GUESSES = 5;
    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 10;
}
