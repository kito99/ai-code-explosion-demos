package com.example.dukeguess.game;

/// How an accepted guess compares to the secret number.
public enum GuessVerdict {

    TOO_LOW,
    TOO_HIGH,
    CORRECT;

    public static GuessVerdict compare(final int guess, final int secretNumber) {
        if (guess < secretNumber) {
            return TOO_LOW;
        }
        return guess > secretNumber ? TOO_HIGH : CORRECT;
    }
}
