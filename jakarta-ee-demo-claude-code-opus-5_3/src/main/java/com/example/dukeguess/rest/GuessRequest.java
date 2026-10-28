package com.example.dukeguess.rest;

/// A guess exactly as the player typed it, so the server can explain anything
/// that is not a whole number in the playable range.
public record GuessRequest(String guess) {
}
