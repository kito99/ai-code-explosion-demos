package com.example.guess.game;

/**
 * Immutable value capturing the outcome of a single accepted guess.
 */
public record GuessResult(
        Feedback feedback,
        int      guessesMade,
        int      guessesRemaining,
        int      secretNumber      // revealed only on lose or win
) {
    public enum Feedback {
        TOO_HIGH, TOO_LOW, CORRECT, LOST
    }
}
