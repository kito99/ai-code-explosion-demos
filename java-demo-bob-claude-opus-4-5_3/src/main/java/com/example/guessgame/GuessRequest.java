package com.example.guessgame;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for submitting a guess.
 */
@Schema(description = "A single guess submission")
public record GuessRequest(
        @Schema(description = "The guessed number, must be between 1 and 10 inclusive")
        @NotNull
        @Min(1)
        @Max(10)
        Integer number
) {}
