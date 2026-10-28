package com.example.dukeguess.web;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.dukeguess.game.GameSnapshot;
import com.example.dukeguess.game.GameStatus;

/// Where a round stands, as the browser sees it.
///
/// @param gameId       identifies the round on later requests
/// @param status       whether the round is still playable
/// @param guessesMade  accepted guesses so far
/// @param guessesRemaining  accepted guesses still allowed
/// @param secretNumber the answer, present only once the round is over
@Schema(name = "Game", description = "The state of a round of the game")
public record GameView(
        UUID gameId,
        GameStatus status,
        int guessesMade,
        int guessesRemaining,
        @Schema(nullable = true) Integer secretNumber) {

    public static GameView of(final GameSnapshot snapshot) {
        return new GameView(
                snapshot.gameId(),
                snapshot.status(),
                snapshot.guessesMade(),
                snapshot.guessesRemaining(),
                snapshot.revealedSecretNumber().orElse(null));
    }
}
