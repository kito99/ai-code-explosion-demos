package com.example.duke.game;

import java.util.UUID;

import static java.lang.String.format;

/// Thrown when a game id does not match a game the server still holds.
public class GameNotFoundException extends GameException {

    public GameNotFoundException(final UUID gameId) {
        super(format("No game found for id %s", gameId));
    }
}
