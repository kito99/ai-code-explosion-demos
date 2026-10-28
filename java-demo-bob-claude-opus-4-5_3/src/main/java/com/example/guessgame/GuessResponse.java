package com.example.guessgame;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response returned after a guess is evaluated.
 */
@Schema(description = "Result of a guess evaluation")
public record GuessResponse(
        @Schema(description = "Outcome: TOO_LOW, TOO_HIGH, CORRECT, or GAME_OVER")
        GuessResult result,
        @Schema(description = "Number of guesses made so far")
        int guessCount,
        @Schema(description = "Number of guesses remaining")
        int guessesRemaining,
        @Schema(description = "The secret number; only present when the game is over")
        Integer secretNumber,
        @Schema(description = "Human-readable message for the player")
        String message
) {}
