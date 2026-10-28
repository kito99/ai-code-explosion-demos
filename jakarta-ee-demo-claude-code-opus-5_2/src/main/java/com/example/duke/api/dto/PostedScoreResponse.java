package com.example.duke.api.dto;

import com.example.duke.api.GameMessages;
import com.example.duke.leaderboard.LeaderboardEntry;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

/// Confirmation that a win was posted. The rank is left out because it depends on everything
/// else posted by then; read the leaderboard to see where the entry landed.
@Schema(name = "PostedScore", description = "A win that was just posted to the leaderboard")
public record PostedScoreResponse(

        @Schema(description = "The name the win was posted under")
        String name,

        @Schema(description = "How many guesses the win took")
        int guessCount,

        @Schema(description = "When the win was posted")
        Instant recordedAt,

        @Schema(description = "Confirmation to show the player")
        String message) {

    public static PostedScoreResponse from(final LeaderboardEntry entry) {
        return new PostedScoreResponse(
                entry.playerName(),
                entry.guessCount(),
                entry.recordedAt(),
                GameMessages.scorePosted(entry.playerName(), entry.guessCount()));
    }
}
