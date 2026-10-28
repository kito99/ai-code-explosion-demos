package com.example.guessduke.web;

import java.util.Optional;

import com.example.guessduke.game.GameStatus;
import com.example.guessduke.game.GuessingGame;

/// Everything the game screen shows about the current round. The secret number is
/// present only once the round has finished, so an active round never leaks it.
public record GameView(
        GameStatus status,
        int guessesMade,
        int guessesRemaining,
        String message,
        Optional<Integer> secretNumber,
        boolean scorePosted) {

    /// Snapshots a round alongside the message describing what just happened.
    public static GameView of(final GuessingGame game, final String message) {
        return new GameView(
                game.status(),
                game.guessesMade(),
                game.guessesRemaining(),
                message,
                game.status().isFinished() ? Optional.of(game.secretNumber()) : Optional.empty(),
                game.isScorePosted());
    }
}
