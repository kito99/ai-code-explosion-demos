package com.example.guessduke.game;

/// How an accepted guess compares to the secret number.
public enum GuessOutcome {

    /// The guess sits below the secret number.
    TOO_LOW,

    /// The guess sits above the secret number.
    TOO_HIGH,

    /// The guess matches the secret number.
    CORRECT
}
