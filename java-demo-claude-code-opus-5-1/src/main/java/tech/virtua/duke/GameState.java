package tech.virtua.duke;

import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;
import static tech.virtua.duke.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.GameStatus.ACTIVE;
import static tech.virtua.duke.GameStatus.LOST;
import static tech.virtua.duke.GameStatus.WON;
import static tech.virtua.duke.GuessFeedback.CORRECT;
import static tech.virtua.duke.GuessFeedback.NONE;
import static tech.virtua.duke.GuessFeedback.REJECTED;
import static tech.virtua.duke.GuessFeedback.TOO_HIGH;
import static tech.virtua.duke.GuessFeedback.TOO_LOW;

/// One game at one moment, and the states it can move to from there. A state
/// never changes once made.
public record GameState(int secretNumber,
                        int guessesMade,
                        GameStatus status,
                        GuessFeedback feedback,
                        OptionalInt lastGuess) {

    public GameState {
        requireNonNull(status);
        requireNonNull(feedback);
        requireNonNull(lastGuess);
    }

    /// A fresh game that has not been guessed at yet.
    public static GameState startWith(final int secretNumber) {
        return new GameState(secretNumber, 0, ACTIVE, NONE, OptionalInt.empty());
    }

    /// Guesses the player still has. Never negative.
    public int guessesRemaining() {
        return Math.max(0, GUESS_LIMIT - guessesMade);
    }

    /// The secret number, present only once the game is over.
    public Optional<Integer> revealedSecretNumber() {
        return status.revealsSecret() ? Optional.of(secretNumber) : Optional.empty();
    }

    /// The state after an accepted guess. A finished game is returned unchanged,
    /// so a late guess neither counts nor reopens play.
    public GameState afterGuess(final int guess) {
        if (!status.acceptsGuesses()) {
            return this;
        }
        final var nextGuessesMade = guessesMade + 1;
        if (guess == secretNumber) {
            return new GameState(secretNumber, nextGuessesMade, WON, CORRECT, OptionalInt.of(guess));
        }
        final var nextStatus = nextGuessesMade >= GUESS_LIMIT ? LOST : ACTIVE;
        final var nextFeedback = guess > secretNumber ? TOO_HIGH : TOO_LOW;
        return new GameState(secretNumber, nextGuessesMade, nextStatus, nextFeedback, OptionalInt.of(guess));
    }

    /// The state after a submission that was not a whole number in range. The
    /// guess count and the previous guess are left alone.
    public GameState afterRejection() {
        if (!status.acceptsGuesses()) {
            return this;
        }
        return new GameState(secretNumber, guessesMade, status, REJECTED, lastGuess);
    }
}
