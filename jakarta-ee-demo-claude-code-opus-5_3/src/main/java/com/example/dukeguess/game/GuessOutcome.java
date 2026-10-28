package com.example.dukeguess.game;

/// Result of a single accepted guess.
public enum GuessOutcome {
    TOO_LOW("Too low! Duke is thinking of something bigger."),
    TOO_HIGH("Too high! Duke is thinking of something smaller."),
    CORRECT("You got it!");

    private final String message;

    GuessOutcome(final String message) {
        this.message = message;
    }

    /// Friendly text describing this outcome to the player.
    public String message() {
        return message;
    }
}
