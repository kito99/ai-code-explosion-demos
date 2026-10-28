package com.example.duke.game;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.example.duke.game.GameRules.MAX_GUESSES;
import static com.example.duke.game.GameStatus.IN_PROGRESS;
import static com.example.duke.game.GameStatus.LOST;
import static com.example.duke.game.GameStatus.WON;

/// One round of the game. Instances are immutable: every accepted guess produces a new `Game`,
/// which lets the store swap states atomically without locking.
public record Game(UUID id,
                   int secretNumber,
                   int guessesMade,
                   GameStatus status,
                   Instant startedAt,
                   boolean scorePosted) {

    public Game {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(startedAt, "startedAt is required");
    }

    /// Creates a fresh round around `secretNumber`.
    public static Game start(final UUID id, final int secretNumber, final Instant startedAt) {
        return new Game(id, secretNumber, 0, IN_PROGRESS, startedAt, false);
    }

    /// Applies an in-range guess and returns the resulting state.
    public Game withGuess(final int guess) {
        if (status.isFinished()) {
            throw new GameAlreadyFinishedException(status);
        }
        final var guesses = guessesMade + 1;
        final var nextStatus = nextStatus(guess, guesses);
        return new Game(id, secretNumber, guesses, nextStatus, startedAt, scorePosted);
    }

    /// Marks this win as recorded on the leaderboard so it cannot be posted twice.
    public Game withScorePosted() {
        if (status != WON) {
            throw new GameNotWonException(status);
        }
        if (scorePosted) {
            throw new ScoreAlreadyPostedException();
        }
        return new Game(id, secretNumber, guessesMade, status, startedAt, true);
    }

    /// Gives the claim back when the win could not be stored, so the player can try again.
    public Game withScorePostingReleased() {
        return scorePosted ? new Game(id, secretNumber, guessesMade, status, startedAt, false) : this;
    }

    /// Guesses the player has left, never below zero.
    public int guessesRemaining() {
        return Math.max(0, MAX_GUESSES - guessesMade);
    }

    /// Duke's number, revealed only once the round is over.
    public Optional<Integer> revealedSecretNumber() {
        return status.isFinished() ? Optional.of(secretNumber) : Optional.empty();
    }

    private GameStatus nextStatus(final int guess, final int guesses) {
        if (guess == secretNumber) {
            return WON;
        }
        return guesses >= MAX_GUESSES ? LOST : IN_PROGRESS;
    }
}
