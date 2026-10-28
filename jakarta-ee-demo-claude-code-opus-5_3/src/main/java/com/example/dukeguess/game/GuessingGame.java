package com.example.dukeguess.game;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.dukeguess.game.GameStatus.ACTIVE;
import static com.example.dukeguess.game.GameStatus.LOST;
import static com.example.dukeguess.game.GameStatus.WON;
import static com.example.dukeguess.game.GuessOutcome.CORRECT;
import static com.example.dukeguess.game.GuessOutcome.TOO_HIGH;
import static com.example.dukeguess.game.GuessOutcome.TOO_LOW;
import static java.lang.String.format;

/// A single round of Guess the Number: holds the secret number, counts accepted
/// guesses, and ends in [GameStatus#WON] or [GameStatus#LOST].
public final class GuessingGame implements Serializable {

    public static final int LOWEST_NUMBER = 1;
    public static final int HIGHEST_NUMBER = 10;
    public static final int MAXIMUM_GUESSES = 5;

    private final int secretNumber;
    private final AtomicInteger guessesMade = new AtomicInteger();
    private final AtomicReference<GameStatus> status = new AtomicReference<>(ACTIVE);

    public GuessingGame(final int secretNumber) {
        if (secretNumber < LOWEST_NUMBER || secretNumber > HIGHEST_NUMBER) {
            throw new GuessOutOfRangeException(format(
                    "Secret number %d is outside %d-%d.", secretNumber, LOWEST_NUMBER, HIGHEST_NUMBER));
        }
        this.secretNumber = secretNumber;
    }

    public static GuessingGame startWith(final SecretNumberGenerator secretNumberGenerator) {
        return new GuessingGame(secretNumberGenerator.nextSecretNumber(LOWEST_NUMBER, HIGHEST_NUMBER));
    }

    /// Records one guess and reports how it compares to the secret number. A guess
    /// outside the playable range is rejected without counting against the limit.
    public GuessOutcome guess(final int guessedNumber) {
        if (status.get() != ACTIVE) {
            throw new GameAlreadyOverException("This game is over. Start a new game to keep playing.");
        }
        if (guessedNumber < LOWEST_NUMBER || guessedNumber > HIGHEST_NUMBER) {
            throw new GuessOutOfRangeException(format(
                    "Please pick a whole number from %d to %d.", LOWEST_NUMBER, HIGHEST_NUMBER));
        }

        final var guessNumber = guessesMade.incrementAndGet();
        if (guessedNumber == secretNumber) {
            status.set(WON);
            return CORRECT;
        }
        if (guessNumber >= MAXIMUM_GUESSES) {
            status.set(LOST);
        }
        return guessedNumber < secretNumber ? TOO_LOW : TOO_HIGH;
    }

    public GameStatus status() {
        return status.get();
    }

    public int guessesMade() {
        return guessesMade.get();
    }

    public int guessesRemaining() {
        return Math.max(0, MAXIMUM_GUESSES - guessesMade.get());
    }

    /// The secret number, revealed only once the game is over.
    public Optional<Integer> revealedSecretNumber() {
        return status.get() == ACTIVE ? Optional.empty() : Optional.of(secretNumber);
    }
}
