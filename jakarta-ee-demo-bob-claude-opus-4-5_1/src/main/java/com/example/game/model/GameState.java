package com.example.game.model;

/**
 * Immutable snapshot of the game state returned to the client after each
 * interaction.
 */
public record GameState(
        String sessionId,
        GameStatus status,
        int guessCount,
        int guessesRemaining,
        GuessFeedback lastFeedback,
        Integer secretNumber   // non-null only in the LOST state
) {}
