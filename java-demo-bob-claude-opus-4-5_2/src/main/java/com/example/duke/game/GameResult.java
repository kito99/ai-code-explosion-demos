package com.example.duke.game;

/**
 * Minimal game outcome snapshot passed to the leaderboard layer.
 * Carries only what's needed to post a score — deliberately omits the secret number.
 */
public record GameResult(
        String gameId,
        GameStatus status,
        int guessesUsed
) {}
