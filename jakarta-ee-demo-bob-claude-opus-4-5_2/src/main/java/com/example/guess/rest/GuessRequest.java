package com.example.guess.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request body for submitting a guess. Jakarta Validation constrains the value
 * at the REST boundary so the resource method receives only valid input.
 */
@Schema(description = "A guess submission")
public record GuessRequest(
    @Schema(description = "The guessed number (1–10)", minimum = "1", maximum = "10", required = true)
    @Min(value = 1, message = "Please enter a whole number between 1 and 10.")
    @Max(value = 10, message = "Please enter a whole number between 1 and 10.")
    int guess
) {}
