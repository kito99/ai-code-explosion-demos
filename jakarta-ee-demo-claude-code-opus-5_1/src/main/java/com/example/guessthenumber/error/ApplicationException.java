package com.example.guessthenumber.error;

import static java.util.Objects.requireNonNull;

/// Base type for errors that carry a player-facing message and a kind the API layer
/// turns into an HTTP status.
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
