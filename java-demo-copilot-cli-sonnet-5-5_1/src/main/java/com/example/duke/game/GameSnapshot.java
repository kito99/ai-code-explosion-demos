package com.example.duke.game;

import java.util.Optional;
import java.util.UUID;

/// Immutable view of a game. The secret is only present once the game is lost.
///
/// @param revealedSecret the secret number, present only in the lost state
public record GameSnapshot(UUID id, GameStatus status, int guessesMade, int guessesRemaining,
                           Optional<Integer> revealedSecret) {
}
