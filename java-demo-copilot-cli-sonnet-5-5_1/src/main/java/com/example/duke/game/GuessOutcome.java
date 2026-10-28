package com.example.duke.game;

public enum GuessOutcome {
    TOO_LOW,
    TOO_HIGH,
    CORRECT;

    public static GuessOutcome of(final int guess, final int secret) {
        return Integer.compare(guess, secret) < 0 ? TOO_LOW : guess == secret ? CORRECT : TOO_HIGH;
    }
}
