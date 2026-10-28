package com.example.duke;

/** The submitted leaderboard name is blank or too long. */
public class InvalidNameException extends DukeException {

    public InvalidNameException(final String message) {
        super(message);
    }
}
