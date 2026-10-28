package com.example.duke.game;

/// Base type for the things that can go wrong while playing a round.
public abstract class GameException extends RuntimeException {

    protected GameException(final String message) {
        super(message);
    }
}
