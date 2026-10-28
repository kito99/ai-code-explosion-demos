package com.example.guess.game;

import java.security.SecureRandom;

/**
 * Holds the state of a single game: secret number, guess counter, and whether the game is active.
 * FR-1, FR-2, FR-3.
 */
public class GameSession {

    public static final int MAX_GUESSES = 5;
    public static final int MIN_NUMBER  = 1;
    public static final int MAX_NUMBER  = 10;

    private static final SecureRandom RNG = new SecureRandom();

    private final int  secretNumber;
    private       int  guessesMade;
    private       boolean active;
    private       boolean won;

    /** Creates a fresh game with a randomly chosen secret number. */
    public GameSession() {
        this.secretNumber = MIN_NUMBER + RNG.nextInt(MAX_NUMBER - MIN_NUMBER + 1);
        this.guessesMade  = 0;
        this.active       = true;
        this.won          = false;
    }

    /**
     * Submits a guess.
     *
     * @param guess a value that MUST already have been validated to be in [1, 10]
     * @return the result of the guess
     * @throws IllegalStateException if the game is no longer active
     */
    public GuessResult guess(int guess) {
        if (!active) {
            throw new IllegalStateException("Game is not active");
        }
        guessesMade++;
        int remaining = MAX_GUESSES - guessesMade;

        if (guess == secretNumber) {
            active = false;
            won    = true;
            return new GuessResult(GuessResult.Feedback.CORRECT, guessesMade, 0, secretNumber);
        }

        if (guessesMade >= MAX_GUESSES) {
            active = false;
            return new GuessResult(
                    guess > secretNumber ? GuessResult.Feedback.TOO_HIGH : GuessResult.Feedback.TOO_LOW,
                    guessesMade, 0, secretNumber);
        }

        GuessResult.Feedback feedback = guess > secretNumber
                ? GuessResult.Feedback.TOO_HIGH
                : GuessResult.Feedback.TOO_LOW;
        return new GuessResult(feedback, guessesMade, remaining, 0);
    }

    public boolean isActive()       { return active; }
    public boolean isWon()          { return won; }
    public int     getGuessesMade() { return guessesMade; }

    /** Exposes the secret only after the game is over. */
    public int getSecretNumber() {
        if (active) throw new IllegalStateException("Secret not revealed until game ends");
        return secretNumber;
    }
}
