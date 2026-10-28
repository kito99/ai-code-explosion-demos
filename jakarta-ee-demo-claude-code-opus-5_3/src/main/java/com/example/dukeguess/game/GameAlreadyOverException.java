package com.example.dukeguess.game;

/// Thrown when a guess arrives after the game has been won or lost.
public class GameAlreadyOverException extends RuntimeException {

    public GameAlreadyOverException(final String message) {
        super(message);
    }
}
