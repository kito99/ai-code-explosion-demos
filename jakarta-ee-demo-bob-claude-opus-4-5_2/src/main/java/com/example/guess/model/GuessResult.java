package com.example.guess.model;

/**
 * Represents the outcome of a single guess submission.
 */
public record GuessResult(
    String sessionId,
    Feedback feedback,
    int guessCount,
    int guessesRemaining,
    Integer secretNumber
) {
    public enum Feedback {
        TOO_LOW, TOO_HIGH, CORRECT, INVALID, GAME_OVER
    }
}
