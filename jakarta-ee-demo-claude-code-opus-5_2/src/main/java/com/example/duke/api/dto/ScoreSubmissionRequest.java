package com.example.duke.api.dto;

import com.example.duke.api.validation.ValidPlayerName;
import com.example.duke.leaderboard.PlayerName;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/// The name a winner posts their score under. It is not an account, and two players may use
/// the same name.
@Schema(name = "ScoreSubmissionRequest", description = "The name to post a win under")
public record ScoreSubmissionRequest(

        @ValidPlayerName
        @Schema(description = "A name with at least one visible character, at most 30 after trimming",
                maxLength = PlayerName.MAX_LENGTH)
        String name) {
}
