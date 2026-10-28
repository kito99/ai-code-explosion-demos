package com.example.duke.web;

import com.example.duke.game.Game;
import com.example.duke.game.GameRules;
import com.example.duke.game.GameStatus;
import com.example.duke.game.GuessOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/// The state of a round as the front-end sees it.
///
/// @param gameId id to send with later guesses
/// @param status whether the round is still in progress, won, or lost
/// @param outcome how the last guess compared, absent when no guess has been made yet
/// @param guessesMade accepted guesses so far
/// @param guessesRemaining guesses still allowed
/// @param secretNumber Duke's number, present only once the round is over
@Schema(name = "Game", description = "State of a round of the game")
public record GameView(UUID gameId,
                       GameStatus status,
                       GuessOutcome outcome,
                       int guessesMade,
                       int guessesRemaining,
                       Integer secretNumber,
                       @Schema(description = "Lowest number Duke can think of", example = "1") int lowestNumber,
                       @Schema(description = "Highest number Duke can think of", example = "10") int highestNumber,
                       @Schema(description = "Guesses allowed per round", example = "5") int maxGuesses) {

    /// Builds a view of a round that has not been guessed at yet.
    public static GameView of(final Game game) {
        return of(game, null);
    }

    /// Builds a view of a round, reporting `outcome` for the guess just made.
    public static GameView of(final Game game, final GuessOutcome outcome) {
        return new GameView(game.id(),
                game.status(),
                outcome,
                game.guessesMade(),
                game.guessesRemaining(),
                game.revealedSecretNumber().orElse(null),
                GameRules.LOWEST_NUMBER,
                GameRules.HIGHEST_NUMBER,
                GameRules.MAX_GUESSES);
    }
}
