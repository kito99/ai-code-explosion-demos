package com.example.guessgame;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for posting a score to the leaderboard.
 */
@Schema(description = "Score submission after winning a game")
public record ScoreRequest(
        @Schema(description = "Player name, 1–30 visible characters after trimming")
        @NotBlank(message = "Name must have at least one visible character.")
        @Size(max = 30, message = "Name must be 30 characters or fewer.")
        String playerName
) {}
