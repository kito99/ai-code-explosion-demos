package com.example.duke;

import java.util.random.RandomGenerator;

/** One round of the guessing game. Not thread-safe; callers synchronize per session. */
public class Game {
    public static final int MIN = 1;
    public static final int MAX = 10;
    public static final int MAX_GUESSES = 5;

    public enum State { ACTIVE, WON, LOST }

    public enum Outcome { TOO_LOW, TOO_HIGH, CORRECT }

    private final int secret;
    private int guessesMade;
    private State state = State.ACTIVE;
    private boolean scorePosted;

    public Game(RandomGenerator random) {
        this.secret = random.nextInt(MIN, MAX + 1);
    }

    /** Records an accepted guess; the caller must have validated the range and checked the game is active. */
    public Outcome guess(int value) {
        if (state != State.ACTIVE) {
            throw new IllegalStateException("Game is over");
        }
        guessesMade++;
        if (value == secret) {
            state = State.WON;
            return Outcome.CORRECT;
        }
        if (guessesMade >= MAX_GUESSES) {
            state = State.LOST;
        }
        return value < secret ? Outcome.TOO_LOW : Outcome.TOO_HIGH;
    }

    public int secret() { return secret; }
    public int guessesMade() { return guessesMade; }
    public int guessesRemaining() { return MAX_GUESSES - guessesMade; }
    public State state() { return state; }
    public boolean scorePosted() { return scorePosted; }
    public void markScorePosted() { scorePosted = true; }
}
