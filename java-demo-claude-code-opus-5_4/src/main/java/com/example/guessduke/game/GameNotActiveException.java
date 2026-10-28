package com.example.guessduke.game;

/// Signals an attempt to act on a round that has already been won or lost.
public class GameNotActiveException extends RuntimeException {

    public GameNotActiveException(final String message) {
        super(message);
    }
}
