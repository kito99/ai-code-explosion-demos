package com.example.dukeguess.rest;

/// Thrown when a score is submitted without an unposted win in the session.
public class NoPostableWinException extends RuntimeException {

    public NoPostableWinException(final String message) {
        super(message);
    }
}
