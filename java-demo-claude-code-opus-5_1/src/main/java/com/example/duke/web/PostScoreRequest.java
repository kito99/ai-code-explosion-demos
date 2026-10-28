package com.example.duke.web;

import io.swagger.v3.oas.annotations.media.Schema;

/// The name a winner wants on the leaderboard.
///
/// @param name display name, trimmed of surrounding whitespace before it is stored
@Schema(name = "PostScoreRequest", description = "Name to record a win under")
public record PostScoreRequest(
        @Schema(description = "Name of 1 to 30 visible characters", example = "Duke Fan", maxLength = 30)
        String name) {
}
