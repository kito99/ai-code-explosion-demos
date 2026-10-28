package com.example.duke.game;

/**
 * Server response after processing a guess.
 */
public record GuessResponse(
        String gameId,
        GameStatus status,
        String feedback,
        int guessesUsed,
        int guessesRemaining,
        Integer revealedNumber
) {}
