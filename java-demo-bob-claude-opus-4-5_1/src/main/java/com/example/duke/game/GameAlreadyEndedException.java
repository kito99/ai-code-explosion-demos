package com.example.duke.game;

/** Thrown when a guess is submitted to a game that has already ended. */
public class GameAlreadyEndedException extends RuntimeException {

    public GameAlreadyEndedException(String gameId) {
        super("Game already ended: " + gameId);
    }
}
