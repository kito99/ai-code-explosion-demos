package com.example.duke.web;

import com.example.duke.game.GameSnapshot;
import com.example.duke.game.GameStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current state of a game.")
public record GameResponse(
        @Schema(description = "Identifier to use for later calls on this game.") String gameId,
        GameStatus status,
        @Schema(description = "Accepted guesses so far.", example = "2") int guessesMade,
        @Schema(description = "Guesses left before the game is lost.", example = "3") int guessesRemaining,
        @Schema(description = "The secret number; only present once the game is lost.", nullable = true)
        Integer secretNumber) {

    static GameResponse from(final GameSnapshot snapshot) {
        return new GameResponse(snapshot.gameId(), snapshot.status(), snapshot.guessesMade(),
                snapshot.guessesRemaining(), snapshot.revealedSecret().orElse(null));
    }
}
