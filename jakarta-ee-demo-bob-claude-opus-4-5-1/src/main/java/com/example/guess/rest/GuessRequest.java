package com.example.guess.rest;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request body for submitting a guess.
 */
@Schema(description = "A guess submission")
public record GuessRequest(
    @Schema(description = "The guessed number (1–10)", minimum = "1", maximum = "10", required = true)
    int guess
) {}
