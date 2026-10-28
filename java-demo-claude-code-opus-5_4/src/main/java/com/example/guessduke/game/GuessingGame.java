package com.example.guessduke.game;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.guessduke.game.GameRules.HIGHEST_NUMBER;
import static com.example.guessduke.game.GameRules.LOWEST_NUMBER;
import static com.example.guessduke.game.GameRules.MAX_GUESSES;
import static com.example.guessduke.game.GameRules.isPlayableNumber;
import static com.example.guessduke.game.GameStatus.ACTIVE;
import static com.example.guessduke.game.GameStatus.LOST;
import static com.example.guessduke.game.GameStatus.WON;
import static com.example.guessduke.game.GuessOutcome.CORRECT;
import static com.example.guessduke.game.GuessOutcome.TOO_HIGH;
import static com.example.guessduke.game.GuessOutcome.TOO_LOW;

/// A single round against Duke. Holds the secret number, counts accepted guesses,
/// and settles into a won or lost status once the round ends. Rejected guesses
/// never reach this class, so every guess it accepts counts against the limit.
///
/// A round lives in one player's session but is read and written by whichever
/// request thread serves that player next. The guess count and the status move
/// together in one atomic step, so two guesses arriving at once can never push the
/// round past its limit or settle it twice.
public final class GuessingGame {

    /// The parts of a round that only make sense together: how many guesses have
    /// been accepted, and whether the round is still open.
    private record RoundState(int guessesMade, GameStatus status) {
    }

    private static final RoundState FRESH_ROUND = new RoundState(0, ACTIVE);

    private final int secretNumber;
    private final AtomicReference<RoundState> roundState = new AtomicReference<>(FRESH_ROUND);
    private final AtomicBoolean scorePosted = new AtomicBoolean();

    public GuessingGame(final int secretNumber) {
        if (!isPlayableNumber(secretNumber)) {
            throw new IllegalArgumentException("A secret number must be between %d and %d, but was %d."
                    .formatted(LOWEST_NUMBER, HIGHEST_NUMBER, secretNumber));
        }
        this.secretNumber = secretNumber;
    }

    /// Scores a guess, counts it against the limit, and ends the round when the
    /// guess is correct or the last guess is used up.
    ///
    /// @throws GameNotActiveException if the round has already finished
    /// @throws InvalidGuessException if the guess falls outside the playable range
    public GuessOutcome submitGuess(final int guess) {
        if (!isPlayableNumber(guess)) {
            throw new InvalidGuessException("Guess %d is outside the playable range.".formatted(guess));
        }

        final var before = roundState.getAndUpdate(
                current -> current.status().isFinished() ? current : afterGuessing(current, guess));
        if (before.status().isFinished()) {
            throw new GameNotActiveException("This round has already finished.");
        }
        return outcomeOf(guess);
    }

    /// Claims the single leaderboard post this round is entitled to, returning `true`
    /// only for the call that claimed it so a double submit cannot record twice.
    ///
    /// @throws GameNotActiveException if the round was not won
    public boolean claimScorePosting() {
        if (roundState.get().status() != WON) {
            throw new GameNotActiveException("Only a won round can post a score.");
        }
        return scorePosted.compareAndSet(false, true);
    }

    /// Hands back a posting slot claimed by [#claimScorePosting()] when the win
    /// could not be stored, so the player can try posting again.
    public void releaseScorePosting() {
        scorePosted.set(false);
    }

    public int secretNumber() {
        return secretNumber;
    }

    public int guessesMade() {
        return roundState.get().guessesMade();
    }

    public int guessesRemaining() {
        return Math.max(0, MAX_GUESSES - roundState.get().guessesMade());
    }

    public GameStatus status() {
        return roundState.get().status();
    }

    public boolean isScorePosted() {
        return scorePosted.get();
    }

    private RoundState afterGuessing(final RoundState current, final int guess) {
        final var guessesMade = current.guessesMade() + 1;
        if (guess == secretNumber) {
            return new RoundState(guessesMade, WON);
        }
        return new RoundState(guessesMade, guessesMade >= MAX_GUESSES ? LOST : ACTIVE);
    }

    private GuessOutcome outcomeOf(final int guess) {
        if (guess == secretNumber) {
            return CORRECT;
        }
        return guess < secretNumber ? TOO_LOW : TOO_HIGH;
    }
}
