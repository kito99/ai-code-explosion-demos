package com.example.dukeguess.rest;

/// Confirmation that a win was recorded.
public record PostedScoreResponse(String playerName, int guessCount, String message) {
}
