package com.example.duke;

/** The requested action is not allowed in the game's current state. */
public class GameStateException extends DukeException {

    public GameStateException(final String message) {
        super(message);
    }
}
