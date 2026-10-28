package com.example.guessgame;

/**
 * Holds the mutable state of a single game session.
 * Stored in the HTTP session so each browser tab has its own game.
 */
public class GameSession {

    private static final int MAX_GUESSES = 5;
    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 10;

    public enum Status { ACTIVE, WON, LOST }

    private int secretNumber;
    private int guessCount;
    private Status status;
    private String lastFeedback;

    public GameSession() {
        reset();
    }

    public void reset() {
        this.secretNumber = MIN_NUMBER + (int) (Math.random() * (MAX_NUMBER - MIN_NUMBER + 1));
        this.guessCount = 0;
        this.status = Status.ACTIVE;
        this.lastFeedback = null;
    }

    /**
     * Process a guess. Returns the feedback message. Updates state.
     * @param guess the player's guess (already validated to be 1–10)
     */
    public String processGuess(int guess) {
        if (status != Status.ACTIVE) {
            return "Game is over. Please start a new game.";
        }

        guessCount++;

        if (guess == secretNumber) {
            status = Status.WON;
            lastFeedback = "Correct! You guessed it in " + guessCount + (guessCount == 1 ? " guess!" : " guesses!");
        } else if (guessCount >= MAX_GUESSES) {
            status = Status.LOST;
            lastFeedback = "No more guesses! The number was " + secretNumber + ".";
        } else if (guess < secretNumber) {
            lastFeedback = "Too low! Try a higher number.";
        } else {
            lastFeedback = "Too high! Try a lower number.";
        }

        return lastFeedback;
    }

    public int getGuessCount() { return guessCount; }
    public int getGuessesRemaining() { return MAX_GUESSES - guessCount; }
    public Status getStatus() { return status; }
    public String getLastFeedback() { return lastFeedback; }
    public int getSecretNumber() { return secretNumber; }
    public int getMaxGuesses() { return MAX_GUESSES; }
}
