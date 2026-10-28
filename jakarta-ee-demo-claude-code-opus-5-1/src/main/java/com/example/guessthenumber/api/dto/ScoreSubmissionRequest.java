package com.example.guessthenumber.api.dto;

import com.example.guessthenumber.leaderboard.PlayerName;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/// The name a winner wants on the leaderboard. The guess count is not part of the
/// request: the server takes it from the caller's own won game.
@Schema(name = "ScoreSubmission", description = "The name to post a win under")
public record ScoreSubmissionRequest(
        @Schema(description = "A name with at least one visible character, trimmed of surrounding whitespace",
                minLength = 1, maxLength = PlayerName.MAX_LENGTH, example = "Ada", required = true)
        String playerName) {
}
