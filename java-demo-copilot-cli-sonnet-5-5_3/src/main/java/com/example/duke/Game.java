package com.example.duke;

import static com.example.duke.GameRules.MAX_GUESSES;
import static com.example.duke.GameRules.MAX_NUMBER;
import static com.example.duke.GameRules.MIN_NUMBER;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/** One round of the guessing game; holds the secret number and enforces guess rules. */
public class Game {

    private final String id;
    private final int secretNumber;
    private final ReentrantLock lock = new ReentrantLock();
    private int guessesMade;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private boolean scorePosted;

    public Game(final String id, final int secretNumber) {
        this.id = id;
        this.secretNumber = secretNumber;
    }

    /** Applies a guess; rejected guesses leave the game unchanged. */
    public GuessResult guess(final int guess) {
        lock.lock();
        try {
            if (status != GameStatus.IN_PROGRESS) {
                throw new GameStateException("This game is over. Start a new game to keep playing.");
            }
            if (guess < MIN_NUMBER || guess > MAX_NUMBER) {
                throw new InvalidGuessException(String.format(
                        "Please enter a whole number from %d to %d.", MIN_NUMBER, MAX_NUMBER));
            }
            guessesMade++;
            if (guess == secretNumber) {
                status = GameStatus.WON;
                return GuessResult.CORRECT;
            }
            if (guessesMade >= MAX_GUESSES) {
                status = GameStatus.LOST;
            }
            return guess < secretNumber ? GuessResult.TOO_LOW : GuessResult.TOO_HIGH;
        } finally {
            lock.unlock();
        }
    }

    /** Marks the win as posted so one game yields at most one leaderboard entry. */
    public int claimScore() {
        lock.lock();
        try {
            if (status != GameStatus.WON) {
                throw new GameStateException("Only a won game can post a score.");
            }
            if (scorePosted) {
                throw new GameStateException("This score was already posted. Start a new game to play again.");
            }
            scorePosted = true;
            return guessesMade;
        } finally {
            lock.unlock();
        }
    }

    /** Reverts a claim when persisting the score fails so the player can retry. */
    public void releaseScore() {
        lock.lock();
        try {
            scorePosted = false;
        } finally {
            lock.unlock();
        }
    }

    public String id() {
        return id;
    }

    public int guessesMade() {
        lock.lock();
        try {
            return guessesMade;
        } finally {
            lock.unlock();
        }
    }

    public int guessesRemaining() {
        return MAX_GUESSES - guessesMade();
    }

    public GameStatus status() {
        lock.lock();
        try {
            return status;
        } finally {
            lock.unlock();
        }
    }

    /** The secret is only revealed once the game has been lost. */
    public Optional<Integer> revealedSecret() {
        return status() == GameStatus.LOST ? Optional.of(secretNumber) : Optional.empty();
    }
}
