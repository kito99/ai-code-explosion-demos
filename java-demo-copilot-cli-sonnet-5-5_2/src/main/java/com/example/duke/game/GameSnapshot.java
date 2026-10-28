package com.example.duke.game;

import java.util.Optional;

/** Immutable view of a game; the secret number is present only once the game is lost. */
public record GameSnapshot(
        String gameId,
        GameStatus status,
        int guessesMade,
        int guessesRemaining,
        Optional<Integer> revealedSecret) {
}
