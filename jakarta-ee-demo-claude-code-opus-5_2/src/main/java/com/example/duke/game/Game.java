package com.example.duke.game;

import java.io.Serializable;
import java.util.Optional;
import java.util.OptionalInt;

import static com.example.duke.game.GameRules.MAX_GUESSES;
import static com.example.duke.game.GameStatus.IN_PROGRESS;
import static com.example.duke.game.GameStatus.LOST;
import static com.example.duke.game.GameStatus.WON;
import static com.example.duke.game.GuessOutcome.CORRECT;
import static com.example.duke.game.GuessOutcome.TOO_HIGH;
import static com.example.duke.game.GuessOutcome.TOO_LOW;

/// One round against Duke. It holds the secret number, counts the guesses that were
/// accepted, and remembers how the last one turned out.
///
/// The secret number stays hidden until the round is over, so a caller cannot read it off a
/// response while the game is still winnable.
public final class Game implements Serializable {

    private final int secretNumber;

    private int guessesMade;
    private GameStatus status = IN_PROGRESS;
    private Integer lastGuess;
    private GuessOutcome lastOutcome;

    /// @throws InvalidGuessException if `secretNumber` is outside the allowed range, which
    ///         would leave a round that cannot be won
    public Game(final int secretNumber) {
        if (!GameRules.isWithinRange(secretNumber)) {
            throw new InvalidGuessException(secretNumber);
        }
        this.secretNumber = secretNumber;
    }

    /// Records a guess and reports how it compares to the secret number. A guess outside the
    /// allowed range is rejected without counting against the limit.
    ///
    /// @throws GameAlreadyOverException if the round has already been won or lost
    /// @throws InvalidGuessException if the guess is outside the allowed range
    public GuessOutcome recordGuess(final int guess) {
        if (status.isOver()) {
            throw new GameAlreadyOverException();
        }
        if (!GameRules.isWithinRange(guess)) {
            throw new InvalidGuessException(guess);
        }

        guessesMade++;
        lastGuess = guess;
        lastOutcome = compareToSecret(guess);

        if (lastOutcome == CORRECT) {
            status = WON;
        } else if (guessesMade >= MAX_GUESSES) {
            status = LOST;
        }
        return lastOutcome;
    }

    private GuessOutcome compareToSecret(final int guess) {
        if (guess == secretNumber) {
            return CORRECT;
        }
        return guess < secretNumber ? TOO_LOW : TOO_HIGH;
    }

    public GameStatus status() {
        return status;
    }

    public int guessesMade() {
        return guessesMade;
    }

    public int guessesRemaining() {
        return MAX_GUESSES - guessesMade;
    }

    public OptionalInt lastGuess() {
        return lastGuess == null ? OptionalInt.empty() : OptionalInt.of(lastGuess);
    }

    public Optional<GuessOutcome> lastOutcome() {
        return Optional.ofNullable(lastOutcome);
    }

    /// The secret number, present only once the round is over.
    public OptionalInt revealedSecretNumber() {
        return status.isOver() ? OptionalInt.of(secretNumber) : OptionalInt.empty();
    }

    /// The number of guesses the win took, present only on a win. This is the figure that
    /// goes on the leaderboard, and it comes from the server's own count rather than from
    /// anything the caller sent.
    public OptionalInt winningGuessCount() {
        return status == WON ? OptionalInt.of(guessesMade) : OptionalInt.empty();
    }
}
