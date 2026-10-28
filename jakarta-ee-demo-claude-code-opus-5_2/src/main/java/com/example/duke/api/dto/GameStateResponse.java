package com.example.duke.api.dto;

import com.example.duke.api.GameMessages;
import com.example.duke.game.GameSnapshot;
import com.example.duke.game.GameStatus;
import com.example.duke.game.GuessOutcome;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.OptionalInt;

/// The round as the player should see it. The fields that do not apply yet are left out of
/// the JSON rather than sent as nulls: `lastGuess` and `lastOutcome` are absent until the
/// first accepted guess, and `secretNumber` stays absent until the round is over.
@Schema(name = "GameState", description = "The state of the game in the caller's session")
public record GameStateResponse(

        @Schema(description = "Whether the round is still being played, won, or lost")
        GameStatus status,

        @Schema(description = "Accepted guesses so far")
        int guessesMade,

        @Schema(description = "Accepted guesses still allowed")
        int guessesRemaining,

        @Schema(description = "The last accepted guess, absent until one is made")
        Integer lastGuess,

        @Schema(description = "How the last accepted guess compared, absent until one is made")
        GuessOutcome lastOutcome,

        @Schema(description = "Duke's number, revealed only once the round is over")
        Integer secretNumber,

        @Schema(description = "Whether this win is still waiting to be posted to the leaderboard")
        boolean canPostScore,

        @Schema(description = "What just happened and what the player can do next")
        String message) {

    public static GameStateResponse from(final GameSnapshot game) {
        return new GameStateResponse(
                game.status(),
                game.guessesMade(),
                game.guessesRemaining(),
                boxed(game.lastGuess()),
                game.lastOutcome().orElse(null),
                boxed(game.revealedSecretNumber()),
                game.canPostScore(),
                GameMessages.forGame(game));
    }

    /// JSON Binding leaves a null field out of the document, which is how a field that does
    /// not apply yet stays absent rather than being sent as null.
    private static Integer boxed(final OptionalInt value) {
        return value.isPresent() ? value.getAsInt() : null;
    }
}
