package com.example.duke;

/** Base type for all application errors. */
public abstract class DukeException extends RuntimeException {

    protected DukeException(final String message) {
        super(message);
    }

    protected DukeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
