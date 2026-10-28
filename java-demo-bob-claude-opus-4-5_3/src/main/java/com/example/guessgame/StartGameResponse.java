package com.example.guessgame;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned when a new game session is started.
 */
@Schema(description = "Game session created in response to a start request")
public record StartGameResponse(
        @Schema(description = "Unique session identifier to use in subsequent calls")
        String sessionId,
        @Schema(description = "Maximum number of guesses allowed per game")
        int maxGuesses
) {}
