package com.example.guess.model;

import java.time.Instant;

/**
 * Leaderboard entry as returned to the client (no internal id exposed).
 */
public record LeaderboardEntryView(
    int rank,
    String playerName,
    int guessCount,
    Instant recordedAt
) {}
