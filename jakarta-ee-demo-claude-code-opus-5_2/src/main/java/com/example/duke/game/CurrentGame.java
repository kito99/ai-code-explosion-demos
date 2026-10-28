package com.example.duke.game;

import jakarta.enterprise.context.SessionScoped;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/// The one game a browser session is playing, plus whether its win has already been posted.
/// An in-progress game lives only here, so it does not survive a restart — which is what the
/// requirements call for.
///
/// The servlet container does not serialise requests that share a session, so two guesses
/// sent at once — a double-click, or a retried request — would otherwise both read the same
/// guess count and both spend it. Every read and write here goes through this session's own
/// lock, and each one hands back a [GameSnapshot] taken while the lock is held, so a caller
/// never reads a round that is halfway through changing.
@SessionScoped
public class CurrentGame implements Serializable {

    private final ReentrantLock lock = new ReentrantLock();

    private Game game;
    private boolean scorePosted;

    /// Replaces any game in progress with a fresh one, which also clears the posted flag so
    /// the new round can post its own win.
    public GameSnapshot replaceWith(final Game freshGame) {
        return inLock(() -> {
            game = freshGame;
            scorePosted = false;
            return snapshotOfCurrent();
        });
    }

    /// The round the session is playing, as it stands right now.
    ///
    /// @throws NoGameInProgressException if the session has never started one
    public GameSnapshot snapshot() {
        return inLock(this::snapshotOfCurrent);
    }

    /// Takes a guess and hands back the round as it stands afterwards.
    ///
    /// @throws NoGameInProgressException if the session has never started a game
    /// @throws GameAlreadyOverException if the round has already been won or lost
    /// @throws InvalidGuessException if the guess is outside the allowed range
    public GameSnapshot recordGuess(final int guess) {
        return inLock(() -> {
            requiredGame().recordGuess(guess);
            return snapshotOfCurrent();
        });
    }

    /// Claims the right to post this session's win, so that only the first of two requests
    /// arriving together gets to record it.
    ///
    /// @return how many guesses the win took
    /// @throws NoGameInProgressException if the session has never started a game
    /// @throws NoWinningGameException if the session's game was not won
    /// @throws ScoreAlreadyPostedException if this win is already claimed
    public int claimWin() {
        return inLock(() -> {
            final var guessCount = requiredGame().winningGuessCount()
                    .orElseThrow(NoWinningGameException::new);
            if (scorePosted) {
                throw new ScoreAlreadyPostedException();
            }
            scorePosted = true;
            return guessCount;
        });
    }

    /// Gives a claim back after recording it failed, so the player can try again.
    public void releaseClaim() {
        inLock(() -> {
            scorePosted = false;
            return null;
        });
    }

    private GameSnapshot snapshotOfCurrent() {
        final var current = requiredGame();
        return GameSnapshot.of(current, current.winningGuessCount().isPresent() && !scorePosted);
    }

    private Game requiredGame() {
        if (game == null) {
            throw new NoGameInProgressException();
        }
        return game;
    }

    private <T> T inLock(final Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
