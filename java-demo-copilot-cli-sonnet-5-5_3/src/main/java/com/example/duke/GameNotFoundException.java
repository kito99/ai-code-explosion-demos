package com.example.duke;

/** No game exists for the requested id. */
public class GameNotFoundException extends DukeException {

    public GameNotFoundException(final String gameId) {
        super(String.format("Game %s was not found.", gameId));
    }
}
