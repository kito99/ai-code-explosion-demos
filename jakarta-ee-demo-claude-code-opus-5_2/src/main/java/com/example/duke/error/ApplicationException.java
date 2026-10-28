package com.example.duke.error;

import static java.util.Objects.requireNonNull;

/// Base type for failures the application reports to a player. The message is player-facing,
/// so it is written in the same friendly tone as the rest of the game.
public abstract class ApplicationException extends RuntimeException {

    private final ErrorKind kind;

    protected ApplicationException(final ErrorKind kind, final String message) {
        super(requireNonNull(message, "message"));
        this.kind = requireNonNull(kind, "kind");
    }

    public ErrorKind kind() {
        return kind;
    }
}
