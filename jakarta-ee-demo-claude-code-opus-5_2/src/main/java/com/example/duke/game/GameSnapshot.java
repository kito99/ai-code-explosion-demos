package com.example.duke.game;

import java.util.Optional;
import java.util.OptionalInt;

/// A round as it stood at one moment, taken while no other request could change it. The game
/// itself is mutable and shared by every request on a session, so callers outside this
/// package work from a snapshot rather than reading a moving target field by field.
public record GameSnapshot(
        GameStatus status,
        int guessesMade,
        int guessesRemaining,
        OptionalInt lastGuess,
        Optional<GuessOutcome> lastOutcome,
        OptionalInt revealedSecretNumber,
        boolean canPostScore) {

    public static GameSnapshot of(final Game game, final boolean canPostScore) {
        return new GameSnapshot(
                game.status(),
                game.guessesMade(),
                game.guessesRemaining(),
                game.lastGuess(),
                game.lastOutcome(),
                game.revealedSecretNumber(),
                canPostScore);
    }
}
