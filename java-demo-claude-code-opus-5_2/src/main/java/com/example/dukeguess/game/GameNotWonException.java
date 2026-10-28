package com.example.dukeguess.game;

import java.util.Locale;

/// Raised when a score is posted for a round the player has not won.
public class GameNotWonException extends GameException {

    public GameNotWonException(final GameStatus status) {
        super(String.format("Only a won game can post a score, and this one is %s.",
                status.name().toLowerCase(Locale.ROOT)));
    }
}
