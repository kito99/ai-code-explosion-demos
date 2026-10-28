package com.example.duke.game;

/// A refused game action, carrying a player-friendly message.
public class GameException extends RuntimeException {

    private final GameError error;

    public GameException(final GameError error, final String message) {
        super(message);
        this.error = error;
    }

    public GameError error() {
        return error;
    }
}
