package com.example.dukeguess.game;

/// Thrown when a guess falls outside the playable range.
public class GuessOutOfRangeException extends RuntimeException {

    public GuessOutOfRangeException(final String message) {
        super(message);
    }
}
