package tech.virtua.duke;

import static java.lang.String.format;

/// Thrown when every session slot is taken by an active player, so a new game
/// cannot be tracked right now.
public class TooManySessionsException extends RuntimeException {

    public TooManySessionsException(final int sessionLimit) {
        super(format("All %d session slots are in use.", sessionLimit));
    }
}
