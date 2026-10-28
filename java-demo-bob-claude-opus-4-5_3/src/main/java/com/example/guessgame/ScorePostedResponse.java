package com.example.guessgame;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response returned after a score is successfully posted to the leaderboard.
 */
@Schema(description = "Confirmation that a score was recorded")
public record ScorePostedResponse(
        @Schema(description = "Player name as stored")
        String playerName,
        @Schema(description = "Number of guesses the win took")
        int guessCount,
        @Schema(description = "When the result was recorded")
        Instant recordedAt
) {}
