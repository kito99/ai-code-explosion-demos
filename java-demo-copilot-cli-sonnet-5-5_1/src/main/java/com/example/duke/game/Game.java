package com.example.duke.game;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.duke.game.GameRules.MAX_GUESSES;

/// A single round. Guesses are accepted only while in progress; the first correct guess wins and
/// the guess that exhausts the limit loses. All state changes are serialised by a lock.
public class Game {

    private final UUID id = UUID.randomUUID();
    private final int secret;
    private final ReentrantLock lock = new ReentrantLock();
    private GameStatus status = GameStatus.IN_PROGRESS;
    private int guessesMade;
    private boolean scorePosted;
    private Instant lastTouched;

    public Game(final int secret, final Instant now) {
        this.secret = secret;
        this.lastTouched = now;
    }

    public UUID id() {
        return id;
    }

    public GuessResult guess(final int guess, final Instant now) {
        lock.lock();
        try {
            lastTouched = now;
            if (status != GameStatus.IN_PROGRESS) {
                throw new GameException(GameError.GAME_FINISHED,
                        "This game is over. Start a new game to keep playing.");
            }
            if (!GameRules.isInRange(guess)) {
                throw new GameException(GameError.INVALID_GUESS,
                        "Please enter a whole number from %d to %d.".formatted(GameRules.MIN_NUMBER, GameRules.MAX_NUMBER));
            }
            guessesMade++;
            final var outcome = GuessOutcome.of(guess, secret);
            if (outcome == GuessOutcome.CORRECT) {
                status = GameStatus.WON;
            } else if (guessesMade >= MAX_GUESSES) {
                status = GameStatus.LOST;
            }
            return new GuessResult(outcome, snapshot());
        } finally {
            lock.unlock();
        }
    }

    /// Marks the win as posted and returns how many guesses it took.
    public int claimScore(final Instant now) {
        lock.lock();
        try {
            lastTouched = now;
            if (status != GameStatus.WON) {
                throw new GameException(GameError.GAME_NOT_WON, "Only a won game can post a score.");
            }
            if (scorePosted) {
                throw new GameException(GameError.SCORE_ALREADY_POSTED, "This score has already been posted.");
            }
            scorePosted = true;
            return guessesMade;
        } finally {
            lock.unlock();
        }
    }

    public void releaseScore() {
        lock.lock();
        try {
            scorePosted = false;
        } finally {
            lock.unlock();
        }
    }

    public GameSnapshot snapshot() {
        lock.lock();
        try {
            return new GameSnapshot(id, status, guessesMade, MAX_GUESSES - guessesMade,
                    status == GameStatus.LOST ? Optional.of(secret) : Optional.empty());
        } finally {
            lock.unlock();
        }
    }

    public boolean isIdleSince(final Instant cutoff) {
        lock.lock();
        try {
            return lastTouched.isBefore(cutoff);
        } finally {
            lock.unlock();
        }
    }
}
