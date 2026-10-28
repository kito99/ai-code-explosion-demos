package com.example.guessthenumber.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/// A guess submitted against the caller's current game.
@Schema(name = "GuessRequest", description = "A guess at Duke's secret number")
public record GuessRequest(
        @Schema(description = "A whole number within the game's range",
                minimum = "1", maximum = "10", example = "7", required = true)
        Integer guess) {
}
