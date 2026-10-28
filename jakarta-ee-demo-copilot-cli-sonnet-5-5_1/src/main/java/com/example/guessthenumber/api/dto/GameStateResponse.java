package com.example.guessthenumber.api.dto;

import com.example.guessthenumber.game.Game;
import com.example.guessthenumber.game.GameStatus;
import com.example.guessthenumber.game.GuessOutcome;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/// Everything the game screen needs after any game action: where the game stands, how
/// the last guess compared, and the sentence to show the player.
@Schema(name = "GameState", description = "The state of the caller's game")
public record GameStateResponse(
        @Schema(description = "Whether the game is still active, won, or lost", required = true)
        GameStatus status,

        @Schema(description = "Accepted guesses made so far", example = "2", required = true)
        int guessesMade,

        @Schema(description = "Accepted guesses still allowed", example = "3", required = true)
        int guessesRemaining,

        @Schema(description = "How the last guess compared; absent until a guess is accepted")
        GuessOutcome lastGuessOutcome,

        @Schema(description = "Duke's number, revealed only once the game is over", example = "7")
        Integer secretNumber,

        @Schema(description = "Whether this win is already on the leaderboard", required = true)
        boolean scorePosted,

        @Schema(description = "What just happened and what the player can do next", required = true)
        String message) {

    public static GameStateResponse of(final Game game, final GuessOutcome lastGuessOutcome, final String message) {
        final var secretNumber = game.revealedSecretNumber();
        return new GameStateResponse(
                game.status(),
                game.guessesMade(),
                game.guessesRemaining(),
                lastGuessOutcome,
                secretNumber.isPresent() ? secretNumber.getAsInt() : null,
                game.isScorePosted(),
                message);
    }
}
