package com.example.dukeguess.game;

import java.util.Optional;
import java.util.UUID;

/// A read-only view of a round at one moment, safe to hand outside the game package.
///
/// The secret number is present only once the round is over, so an in-progress snapshot
/// can never leak the answer.
public record GameSnapshot(
        UUID gameId,
        GameStatus status,
        int guessesMade,
        int guessesRemaining,
        Optional<Integer> revealedSecretNumber) {
}
