package com.example.duke.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Payload the client sends when submitting a guess. */
public record GuessRequest(
        @NotNull(message = "gameId is required")
        String gameId,

        @NotNull(message = "guess is required")
        @Min(value = Game.MIN_NUMBER, message = "Guess must be between 1 and 10")
        @Max(value = Game.MAX_NUMBER, message = "Guess must be between 1 and 10")
        Integer guess
) {}
