package com.example.duke.model;

import java.time.Instant;

/**
 * An immutable record of a winning game posted to the leaderboard.
 */
public record LeaderboardEntry(
        long id,
        String playerName,
        int guessCount,
        Instant recordedAt
) {
    public LeaderboardEntry {
        playerName = playerName.strip();
    }
}
