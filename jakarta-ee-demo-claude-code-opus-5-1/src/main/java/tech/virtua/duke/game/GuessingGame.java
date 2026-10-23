package tech.virtua.duke.game;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.OptionalInt;

import static tech.virtua.duke.game.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.game.GameStatus.ACTIVE;
import static tech.virtua.duke.game.GameStatus.LOST;
import static tech.virtua.duke.game.GameStatus.WON;
import static tech.virtua.duke.game.GuessFeedback.CORRECT;
import static tech.virtua.duke.game.GuessFeedback.NONE;
import static tech.virtua.duke.game.GuessFeedback.TOO_HIGH;
import static tech.virtua.duke.game.GuessFeedback.TOO_LOW;
import static tech.virtua.duke.game.SubmissionOutcome.COUNTED;
import static tech.virtua.duke.game.SubmissionOutcome.GAME_ALREADY_OVER;
import static tech.virtua.duke.game.SubmissionOutcome.NOT_A_VALID_NUMBER;

/// One round of the game, from the opening draw to the win or the loss. A game never
/// changes: every submission answers with the game that follows it, so no reader can
/// catch one half way through a guess.
public final class GuessingGame implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int secretNumber;
    private final int guessesMade;
    private final GameStatus status;
    private final GuessFeedback feedback;

    /// Null until a guess has counted. Held boxed rather than as an [OptionalInt] so a
    /// game can ride along in a session that the server decides to write out.
    private final Integer lastGuess;

    private GuessingGame(final int secretNumber, final int guessesMade, final GameStatus status,
                         final GuessFeedback feedback, final Integer lastGuess) {
        if (!GameRules.isGuessable(secretNumber)) {
            throw new SecretNumberOutOfRangeException(secretNumber);
        }
        this.secretNumber = secretNumber;
        this.guessesMade = guessesMade;
        this.status = Objects.requireNonNull(status);
        this.feedback = Objects.requireNonNull(feedback);
        this.lastGuess = lastGuess;
    }

    /// Deals a fresh game around a known secret number.
    public static GuessingGame startWith(final int secretNumber) {
        return new GuessingGame(secretNumber, 0, ACTIVE, NONE, null);
    }

    /// Deals a fresh game around a newly drawn secret number.
    public static GuessingGame dealtBy(final SecretNumberPicker picker) {
        return startWith(picker.pickSecretNumber());
    }

    /// Offers the game whatever the player typed, and reports what it made of it.
    public Submission submit(final String rawGuess) {
        if (status.isOver()) {
            return new Submission(GAME_ALREADY_OVER, this);
        }
        final var guess = GuessParser.parse(rawGuess);
        if (guess.isEmpty()) {
            return new Submission(NOT_A_VALID_NUMBER, this);
        }
        return new Submission(COUNTED, afterGuess(guess.getAsInt()));
    }

    /// The number Duke is thinking of.
    public int secretNumber() {
        return secretNumber;
    }

    /// How many guesses have counted so far.
    public int guessesMade() {
        return guessesMade;
    }

    /// How many guesses the player has left.
    public int guessesRemaining() {
        return GUESS_LIMIT - guessesMade;
    }

    /// Where the game stands.
    public GameStatus status() {
        return status;
    }

    /// How the last counted guess compared to the secret number.
    public GuessFeedback feedback() {
        return feedback;
    }

    /// The last guess that counted, empty until one has.
    public OptionalInt lastGuess() {
        return lastGuess == null ? OptionalInt.empty() : OptionalInt.of(lastGuess);
    }

    /// The game as the player may see it.
    public GameSnapshot snapshot() {
        final var revealedSecret = status.isOver() ? OptionalInt.of(secretNumber) : OptionalInt.empty();
        return new GameSnapshot(status, feedback, guessesMade, guessesRemaining(), lastGuess(), revealedSecret);
    }

    private GuessingGame afterGuess(final int guess) {
        final var guessesNowMade = guessesMade + 1;
        final var newFeedback = feedbackFor(guess);
        return new GuessingGame(secretNumber, guessesNowMade,
                statusFor(newFeedback, guessesNowMade), newFeedback, guess);
    }

    private GuessFeedback feedbackFor(final int guess) {
        if (guess == secretNumber) {
            return CORRECT;
        }
        return guess < secretNumber ? TOO_LOW : TOO_HIGH;
    }

    private static GameStatus statusFor(final GuessFeedback feedback, final int guessesMade) {
        if (feedback == CORRECT) {
            return WON;
        }
        return guessesMade >= GUESS_LIMIT ? LOST : ACTIVE;
    }
}
