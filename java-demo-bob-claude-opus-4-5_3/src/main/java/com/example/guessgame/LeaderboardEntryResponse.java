package com.example.guessgame;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Leaderboard entry as returned by the API.
 */
@Schema(description = "A single leaderboard entry")
public record LeaderboardEntryResponse(
        @Schema(description = "Rank position (1-based)")
        int rank,
        @Schema(description = "Player name")
        String playerName,
        @Schema(description = "Number of guesses the win took")
        int guessCount,
        @Schema(description = "When the result was recorded")
        Instant recordedAt
) {}
