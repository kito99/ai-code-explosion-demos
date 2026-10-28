package com.example.duke.leaderboard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for posting a score after a win.
 * Name length validation (1-30 visible chars after trim, per FR-14) is enforced
 * by LeaderboardService so the limit applies to the trimmed value, not the raw string.
 */
public record PostScoreRequest(
        @NotNull(message = "gameId is required")
        String gameId,

        @NotBlank(message = "Name must not be blank")
        String playerName
) {}
