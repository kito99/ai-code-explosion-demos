package com.example.guessduke.web;

/// A guess exactly as the player typed it, so the server decides what counts.
public record GuessRequest(String guess) {
}
