package com.example.duke.web;

import com.example.duke.game.GameSnapshot;
import com.example.duke.game.GameStatus;
import com.example.duke.game.GuessOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "The state of a game.")
public record GameResponse(
        UUID gameId,
        GameStatus status,
        int guessesMade,
        int guessesRemaining,
        @Schema(description = "Result of the latest guess; absent when a game has just started.", nullable = true)
        GuessOutcome outcome,
        @Schema(description = "The secret number; present only once the game is lost.", nullable = true)
        Integer secretNumber,
        String message) {

    static GameResponse of(final GameSnapshot game, final GuessOutcome outcome, final String message) {
        return new GameResponse(game.id(), game.status(), game.guessesMade(), game.guessesRemaining(),
                outcome, game.revealedSecret().orElse(null), message);
    }
}
