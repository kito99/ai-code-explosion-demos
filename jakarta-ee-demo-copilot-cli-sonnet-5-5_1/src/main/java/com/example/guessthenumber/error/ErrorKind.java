package com.example.guessthenumber.error;

/// Classifies an application error so the API layer can pick an HTTP status without
/// the domain packages depending on Jakarta REST.
public enum ErrorKind {
    /// The caller sent something the application cannot accept.
    INVALID_INPUT,

    /// The request is well formed but conflicts with the state of the player's game.
    CONFLICT,

    /// The requested thing does not exist.
    NOT_FOUND,

    /// The application failed for a reason the caller cannot do anything about.
    INTERNAL
}
