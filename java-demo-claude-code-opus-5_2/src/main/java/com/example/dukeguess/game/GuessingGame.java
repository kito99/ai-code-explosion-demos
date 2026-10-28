package com.example.dukeguess.game;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.dukeguess.game.GameRules.MAX_GUESSES;

/// One round of the game: the number Duke is thinking of, the guesses spent on it, and
/// whether the round was won, lost, or is still open.
///
/// Instances are shared across requests, so guess handling runs under a lock and the
/// leaderboard claim is a single atomic step.
public final class GuessingGame {

    private final UUID id;
    private final int secretNumber;
    private final Instant startedAt;
    private final ReentrantLock guessLock = new ReentrantLock();
    private final AtomicBoolean scoreClaimed = new AtomicBoolean();

    private volatile int guessesMade;
    private volatile GameStatus status = GameStatus.IN_PROGRESS;

    GuessingGame(final UUID id, final int secretNumber, final Instant startedAt) {
        this.id = id;
        this.secretNumber = secretNumber;
        this.startedAt = startedAt;
    }

    public UUID id() {
        return id;
    }

    public GameStatus status() {
        return status;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public int guessesMade() {
        return guessesMade;
    }

    public int guessesRemaining() {
        return MAX_GUESSES - guessesMade;
    }

    /// Scores one guess against the secret number and advances the round.
    ///
    /// Throws [InvalidGuessException] for a guess outside the playable range and
    /// [GameAlreadyOverException] once the round is over; neither spends a guess.
    public GuessOutcome submitGuess(final int guess) {
        GameRules.requireGuessInRange(guess);
        guessLock.lock();
        try {
            if (status.isOver()) {
                throw new GameAlreadyOverException(status);
            }
            guessesMade++;
            final var verdict = GuessVerdict.compare(guess, secretNumber);
            status = nextStatus(verdict);
            return new GuessOutcome(verdict, snapshot());
        } finally {
            guessLock.unlock();
        }
    }

    private GameStatus nextStatus(final GuessVerdict verdict) {
        if (verdict == GuessVerdict.CORRECT) {
            return GameStatus.WON;
        }
        return guessesRemaining() == 0 ? GameStatus.LOST : GameStatus.IN_PROGRESS;
    }

    /// Takes ownership of this round's single leaderboard entry, returning `false` when
    /// another request already holds it. Claiming before the write means two posts of the
    /// same win can never both land.
    public boolean claimScorePost() {
        return scoreClaimed.compareAndSet(false, true);
    }

    /// Hands the claim back when the write did not land, so the player can try again.
    public void releaseScorePost() {
        scoreClaimed.set(false);
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(id, status, guessesMade, guessesRemaining(), revealedSecretNumber());
    }

    /// The secret number, present only once the round is over.
    public Optional<Integer> revealedSecretNumber() {
        return status.isOver() ? Optional.of(secretNumber) : Optional.empty();
    }
}
