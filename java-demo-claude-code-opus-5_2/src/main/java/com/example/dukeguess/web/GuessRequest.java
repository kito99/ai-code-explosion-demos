package com.example.dukeguess.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import com.example.dukeguess.game.GameRules;

/// A guess the player submitted.
///
/// The range is checked by the game rules rather than by bean validation, so an
/// out-of-range guess comes back with the same friendly message however it arrives.
@Schema(name = "GuessRequest")
public record GuessRequest(
        @NotNull
        @Schema(description = "A whole number Duke might be thinking of",
                minimum = "" + GameRules.LOWEST_NUMBER,
                maximum = "" + GameRules.HIGHEST_NUMBER,
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer guess) {
}
