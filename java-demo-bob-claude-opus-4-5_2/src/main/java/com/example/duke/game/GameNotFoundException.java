package com.example.duke.game;

/** Thrown when a game ID does not match any known session. */
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String gameId) {
        super("Game not found: " + gameId);
    }
}
