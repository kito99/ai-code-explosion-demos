package com.example.guessthenumber.game;

/// How an accepted guess compared with the secret number.
public enum GuessOutcome {
    /// The guess was below the secret number.
    TOO_LOW,

    /// The guess was above the secret number.
    TOO_HIGH,

    /// The guess matched the secret number.
    CORRECT
}
