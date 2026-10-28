package com.example.dukeguess.game;

import java.util.UUID;

/// Raised when a request names a round the server no longer holds.
public class GameNotFoundException extends GameException {

    public GameNotFoundException(final UUID gameId) {
        super(String.format("No game with id %s is in play.", gameId));
    }
}
