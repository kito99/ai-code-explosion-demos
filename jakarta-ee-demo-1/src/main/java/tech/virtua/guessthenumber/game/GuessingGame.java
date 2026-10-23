package tech.virtua.guessthenumber.game;

import java.io.Serial;
import java.io.Serializable;
import java.util.OptionalInt;
import java.util.random.RandomGenerator;

/**
 * One round of Guess the Number. The rules live here, free of any container or
 * transport concerns, so they can be exercised directly from unit tests.
 */
public final class GuessingGame implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Lowest number the secret can be, and the lowest guess the game accepts. */
    public static final int MIN_NUMBER = 1;
    /** Highest number the secret can be, and the highest guess the game accepts. */
    public static final int MAX_NUMBER = 10;
    /** Accepted guesses allowed per game. */
    public static final int MAX_GUESSES = 5;

    private final int secretNumber;
    private int guessesMade;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private Integer lastGuess;
    private Hint lastHint;

    /**
     * Creates a game with a known secret number. Package-private so production code
     * goes through {@link #withRandomSecret}, while tests can pin the secret.
     *
     * @throws IllegalArgumentException if the secret is outside the playable range
     */
    GuessingGame(int secretNumber) {
        if (!isInRange(secretNumber)) {
            throw new IllegalArgumentException(
                    "Secret number must be between " + MIN_NUMBER + " and " + MAX_NUMBER
                            + ", but was " + secretNumber);
        }
        this.secretNumber = secretNumber;
    }

    /**
     * Starts a game whose secret is drawn uniformly from {@value #MIN_NUMBER} through
     * {@value #MAX_NUMBER}. Draws are independent, so repeats across games are normal.
     */
    public static GuessingGame withRandomSecret(RandomGenerator random) {
        return new GuessingGame(random.nextInt(MIN_NUMBER, MAX_NUMBER + 1));
    }

    /**
     * Reads raw player input as a guess. Anything that is not a whole number inside
     * the playable range is rejected, which keeps invalid input from costing a guess.
     *
     * @param rawInput text the player submitted; may be {@code null}
     * @return the parsed guess, or empty if the input is not a usable guess
     */
    public static OptionalInt parseGuess(String rawInput) {
        if (rawInput == null) {
            return OptionalInt.empty();
        }
        String trimmed = rawInput.strip();
        if (trimmed.isEmpty()) {
            return OptionalInt.empty();
        }
        // Plain ASCII digits only. Integer.parseInt would also take a leading sign and
        // non-Latin digit characters, none of which a player means as a guess here.
        if (!trimmed.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return OptionalInt.empty();
        }
        try {
            int guess = Integer.parseInt(trimmed);
            return isInRange(guess) ? OptionalInt.of(guess) : OptionalInt.empty();
        } catch (NumberFormatException tooManyDigits) {
            return OptionalInt.empty();
        }
    }

    private static boolean isInRange(int value) {
        return value >= MIN_NUMBER && value <= MAX_NUMBER;
    }

    /**
     * Counts a guess and reports how it compared to the secret number.
     *
     * @throws IllegalArgumentException if the guess is outside the playable range
     * @throws IllegalStateException    if the game has already been won or lost
     */
    public GuessOutcome guess(int guess) {
        if (!isInRange(guess)) {
            throw new IllegalArgumentException(
                    "Guess must be between " + MIN_NUMBER + " and " + MAX_NUMBER
                            + ", but was " + guess);
        }
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("This game is already over: " + status);
        }

        guessesMade++;
        lastGuess = guess;

        if (guess == secretNumber) {
            lastHint = Hint.CORRECT;
            status = GameStatus.WON;
        } else {
            lastHint = guess > secretNumber ? Hint.TOO_HIGH : Hint.TOO_LOW;
            if (guessesMade >= MAX_GUESSES) {
                status = GameStatus.LOST;
            }
        }

        return new GuessOutcome(guess, lastHint, status);
    }

    public GameStatus status() {
        return status;
    }

    public boolean isOver() {
        return status != GameStatus.IN_PROGRESS;
    }

    public int guessesMade() {
        return guessesMade;
    }

    /** Guesses the player has left, never below zero. */
    public int guessesRemaining() {
        return Math.max(0, MAX_GUESSES - guessesMade);
    }

    /** The most recent accepted guess, or {@code null} before the first one. */
    public Integer lastGuess() {
        return lastGuess;
    }

    /** How the most recent accepted guess compared, or {@code null} before the first one. */
    public Hint lastHint() {
        return lastHint;
    }

    /**
     * The secret number. Only call this once the game is over; while a game is in
     * progress it must not reach the player.
     */
    public int secretNumber() {
        return secretNumber;
    }

    /**
     * Takes an immutable read of the game. The secret number is included only once
     * the game is over, so an in-progress snapshot can be handed to the player as is.
     */
    public GameSnapshot snapshot() {
        return new GameSnapshot(
                status,
                guessesMade,
                guessesRemaining(),
                lastGuess,
                lastHint,
                isOver() ? secretNumber : null);
    }
}
