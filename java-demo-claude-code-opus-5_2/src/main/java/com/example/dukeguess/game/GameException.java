package com.example.dukeguess.game;

/// Base type for the ways a game request can be rejected by the rules.
public abstract class GameException extends RuntimeException {

    protected GameException(final String message) {
        super(message);
    }
}
