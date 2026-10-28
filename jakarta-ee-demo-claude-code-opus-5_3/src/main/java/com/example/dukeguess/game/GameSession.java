package com.example.dukeguess.game;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.annotation.Counted;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.dukeguess.game.GameStatus.WON;

/// The one game a player has in progress, kept for the life of their HTTP session.
@SessionScoped
public class GameSession implements Serializable {

    private final AtomicReference<GuessingGame> currentGame = new AtomicReference<>();
    private final AtomicBoolean scorePosted = new AtomicBoolean();

    @Inject
    private SecretNumberGenerator secretNumberGenerator;

    /// Replaces any game in progress with a fresh one.
    @Counted(name = "duke.games.started", description = "Games started")
    public GuessingGame startNewGame() {
        final var game = GuessingGame.startWith(secretNumberGenerator);
        currentGame.set(game);
        scorePosted.set(false);
        return game;
    }

    /// The game in progress, starting one if the player has not played yet.
    public GuessingGame currentGame() {
        final var game = currentGame.get();
        return game != null ? game : startNewGame();
    }

    public GuessOutcome guess(final int guessedNumber) {
        return currentGame().guess(guessedNumber);
    }

    /// The guess count to post to the leaderboard, present only while an unposted win stands.
    public Optional<Integer> postableWin() {
        final var game = currentGame.get();
        if (game == null || game.status() != WON || scorePosted.get()) {
            return Optional.empty();
        }
        return Optional.of(game.guessesMade());
    }

    /// Claims the current win for posting, returning `false` if it is already claimed.
    public boolean claimWinForPosting() {
        return scorePosted.compareAndSet(false, true);
    }

    /// Hands the win back so the player can try again when recording it fails.
    public void releaseWinForPosting() {
        scorePosted.set(false);
    }
}
