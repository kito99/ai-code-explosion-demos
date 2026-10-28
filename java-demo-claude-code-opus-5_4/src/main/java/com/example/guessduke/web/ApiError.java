package com.example.guessduke.web;

/// A failure the player can read, returned when a request cannot be served at all.
public record ApiError(String message) {
}
