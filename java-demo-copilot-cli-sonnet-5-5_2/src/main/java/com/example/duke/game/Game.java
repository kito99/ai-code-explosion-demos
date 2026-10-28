package com.example.duke.game;

import static com.example.duke.game.GameRules.MAX_GUESSES;
import static com.example.duke.game.GameRules.MAX_NUMBER;
import static com.example.duke.game.GameRules.MIN_NUMBER;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntFunction;

/** A single round of the guessing game. Safe for concurrent use. */
public final class Game {

    private final String gameId;
    private final int secretNumber;
    private final ReentrantLock lock = new ReentrantLock();
    private int guessesMade;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private boolean scorePosted;

    public Game(final String gameId, final int secretNumber) {
        this.gameId = gameId;
        this.secretNumber = secretNumber;
    }

    public String gameId() {
        return gameId;
    }

    public GuessOutcome guess(final int value) {
        lock.lock();
        try {
            if (value < MIN_NUMBER || value > MAX_NUMBER) {
                throw new InvalidGuessException();
            }
            if (status != GameStatus.IN_PROGRESS) {
                throw new GameFinishedException();
            }
            guessesMade++;
            if (value == secretNumber) {
                status = GameStatus.WON;
                return GuessOutcome.CORRECT;
            }
            if (guessesMade >= MAX_GUESSES) {
                status = GameStatus.LOST;
            }
            return value < secretNumber ? GuessOutcome.TOO_LOW : GuessOutcome.TOO_HIGH;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the recorder with the winning guess count while holding the game lock, so a won game
     * can be recorded at most once. The game only counts as posted if the recorder succeeds.
     */
    public <T> T recordWin(final IntFunction<T> recorder) {
        lock.lock();
        try {
            if (status != GameStatus.WON) {
                throw new ScoreNotPostableException("Only a won game can be posted to the leaderboard.");
            }
            if (scorePosted) {
                throw new ScoreNotPostableException("This score is already on the leaderboard.");
            }
            final var result = recorder.apply(guessesMade);
            scorePosted = true;
            return result;
        } finally {
            lock.unlock();
        }
    }

    public GameSnapshot snapshot() {
        lock.lock();
        try {
            final var revealed = status == GameStatus.LOST ? Optional.of(secretNumber) : Optional.<Integer>empty();
            return new GameSnapshot(gameId, status, guessesMade, MAX_GUESSES - guessesMade, revealed);
        } finally {
            lock.unlock();
        }
    }
}
