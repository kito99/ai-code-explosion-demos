package com.example.duke.game;

/// How a single accepted guess compared to Duke's secret number.
public enum GuessOutcome {

    TOO_LOW,
    TOO_HIGH,
    CORRECT;

    /// Compares `guess` against `secretNumber`.
    public static GuessOutcome of(final int guess, final int secretNumber) {
        if (guess < secretNumber) {
            return TOO_LOW;
        }
        return guess > secretNumber ? TOO_HIGH : CORRECT;
    }
}
