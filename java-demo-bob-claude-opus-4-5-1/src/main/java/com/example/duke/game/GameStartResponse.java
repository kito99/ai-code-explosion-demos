package com.example.duke.game;

/**
 * Server response after starting a new game.
 * The secret number is never included.
 */
public record GameStartResponse(
        String gameId,
        int guessesRemaining,
        String message
) {}
