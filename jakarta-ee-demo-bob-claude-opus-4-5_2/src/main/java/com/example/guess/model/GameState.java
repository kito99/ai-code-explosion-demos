package com.example.guess.model;

/**
 * Snapshot of a game session state returned to the client.
 */
public record GameState(
    String sessionId,
    int guessCount,
    int guessesRemaining,
    boolean active,
    boolean won,
    boolean lost
) {}
