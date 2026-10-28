package com.example.guessduke.game;

/// Signals a guess that falls outside the playable range.
public class InvalidGuessException extends RuntimeException {

    public InvalidGuessException(final String message) {
        super(message);
    }
}
