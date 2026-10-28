package com.example.guessgame;

import java.security.SecureRandom;

/**
 * Holds the mutable state of a single in-progress game session.
 * The secret number is chosen at construction; guesses are evaluated via {@link #guess(int)}.
 */
public class Game {

    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 10;
    private static final int MAX_GUESSES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int secretNumber;
    private int guessCount;
    private boolean won;
    private boolean lost;

    public Game() {
        this.secretNumber = MIN_NUMBER + RANDOM.nextInt(MAX_NUMBER - MIN_NUMBER + 1);
        this.guessCount = 0;
        this.won = false;
        this.lost = false;
    }

    /**
     * Evaluates a guess and updates game state accordingly.
     * Returns {@link GuessResult#GAME_OVER} if the game has already ended.
     */
    public GuessResult guess(int number) {
        if (won || lost) {
            return GuessResult.GAME_OVER;
        }
        guessCount++;
        if (number == secretNumber) {
            won = true;
            return GuessResult.CORRECT;
        }
        if (guessCount >= MAX_GUESSES) {
            lost = true;
            return GuessResult.GAME_OVER;
        }
        return number < secretNumber ? GuessResult.TOO_LOW : GuessResult.TOO_HIGH;
    }

    public boolean isOver() {
        return won || lost;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isLost() {
        return lost;
    }

    public int getGuessCount() {
        return guessCount;
    }

    public int getGuessesRemaining() {
        return Math.max(0, MAX_GUESSES - guessCount);
    }

    public int getSecretNumber() {
        return secretNumber;
    }

    public static int getMaxGuesses() {
        return MAX_GUESSES;
    }
}
