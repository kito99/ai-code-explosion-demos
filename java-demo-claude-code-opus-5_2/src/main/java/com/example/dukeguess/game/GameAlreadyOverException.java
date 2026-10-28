package com.example.dukeguess.game;

import java.util.Locale;

/// Raised when a guess arrives for a round that has already been won or lost.
public class GameAlreadyOverException extends GameException {

    public GameAlreadyOverException(final GameStatus status) {
        super(String.format("This game is already %s. Start a new one to keep playing.",
                status.name().toLowerCase(Locale.ROOT)));
    }
}
