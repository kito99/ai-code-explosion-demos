package com.example.duke;

/** The submitted guess is not a whole number within the allowed range. */
public class InvalidGuessException extends DukeException {

    public InvalidGuessException(final String message) {
        super(message);
    }
}
