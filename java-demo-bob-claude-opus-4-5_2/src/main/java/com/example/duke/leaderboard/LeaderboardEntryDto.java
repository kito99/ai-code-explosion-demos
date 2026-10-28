package com.example.duke.leaderboard;

import java.time.Instant;

/** DTO for a single leaderboard row returned by the API. */
public record LeaderboardEntryDto(
        int rank,
        String playerName,
        int guessCount,
        Instant recordedAt
) {}
