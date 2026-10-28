package com.example.duke;

/** Reading or writing leaderboard data failed. */
public class PersistenceException extends DukeException {

    public PersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
