package com.example.dukeguess.web;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.dukeguess.leaderboard.PlayerName;

/// The name a winner wants on the leaderboard.
///
/// Trimming and length are settled by [PlayerName], so the same rules apply whether the
/// name arrives from the game screen or from a direct API call.
@Schema(name = "PostScoreRequest")
public record PostScoreRequest(
        @Schema(description = "Shown on the leaderboard; not an account",
                maxLength = PlayerName.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
