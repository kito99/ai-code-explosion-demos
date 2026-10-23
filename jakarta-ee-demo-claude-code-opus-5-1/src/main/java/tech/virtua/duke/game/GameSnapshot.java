package tech.virtua.duke.game;

import java.util.Objects;
import java.util.OptionalInt;

/// A game as the player is allowed to see it at one moment. The secret number is
/// present only once the game is over, so a snapshot can be handed outside the game
/// package without giving the answer away.
public record GameSnapshot(GameStatus status,
                           GuessFeedback feedback,
                           int guessesMade,
                           int guessesRemaining,
                           OptionalInt lastGuess,
                           OptionalInt secretNumber) {

    public GameSnapshot {
        Objects.requireNonNull(status);
        Objects.requireNonNull(feedback);
        Objects.requireNonNull(lastGuess);
        Objects.requireNonNull(secretNumber);
    }
}
