package com.example.duke.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/// A guess the player submitted.
///
/// @param guess whole number the player thinks Duke picked
@Schema(name = "GuessRequest", description = "A guess at Duke's number")
public record GuessRequest(
        @NotNull(message = "Please enter a whole number between 1 and 10.")
        @Schema(description = "Whole number between 1 and 10", example = "7", minimum = "1", maximum = "10")
        Integer guess) {
}
