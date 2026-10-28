package com.example.guessduke.web;

/// A name exactly as the player typed it, so the server decides what is postable.
public record ScoreRequest(String name) {
}
