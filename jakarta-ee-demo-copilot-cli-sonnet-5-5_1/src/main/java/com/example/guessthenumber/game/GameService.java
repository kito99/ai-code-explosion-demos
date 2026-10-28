package com.example.guessthenumber.game;

import com.example.guessthenumber.telemetry.GameMetrics;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/// Drives the game the player in the current session is playing and reports every
/// lifecycle event to telemetry.
@Singleton
public class GameService {

    private static final Logger LOGGER = Logger.getLogger(GameService.class.getName());

    private final CurrentGame currentGame;
    private final SecretNumberGenerator secretNumbers;
    private final GameMetrics metrics;

    @Inject
    public GameService(final CurrentGame currentGame, final SecretNumberGenerator secretNumbers,
                       final GameMetrics metrics) {
        this.currentGame = requireNonNull(currentGame, "currentGame");
        this.secretNumbers = requireNonNull(secretNumbers, "secretNumbers");
        this.metrics = requireNonNull(metrics, "metrics");
    }

    /// Starts a game under the standard rules, replacing any game already in the session.
    @WithSpan("game.start")
    public Game startNewGame() {
        final var rules = GameRules.STANDARD;
        final var game = new Game(rules, secretNumbers.nextSecretNumber(rules));
        currentGame.replaceWith(game);
        metrics.gameStarted();
        LOGGER.info(() -> "game.started range=%d-%d maxGuesses=%d"
                .formatted(rules.lowestNumber(), rules.highestNumber(), rules.maxGuesses()));
        return game;
    }

    /// @throws NoGameInProgressException if the player has not started a game yet
    public Game requireGame() {
        return currentGame.require();
    }

    /// Applies a guess to the session's game.
    ///
    /// @throws NoGameInProgressException if the player has not started a game yet
    /// @throws GameAlreadyOverException if the game has already been won or lost
    /// @throws InvalidGuessException if the guess is missing or outside the allowed range
    @WithSpan("game.guess")
    public GuessResult submitGuess(final Integer guess) {
        final var game = currentGame.require();
        if (guess == null) {
            metrics.guessRejected();
            throw new InvalidGuessException(game.rules());
        }

        final GuessOutcome outcome;
        try {
            outcome = game.guess(guess);
        } catch (final InvalidGuessException rejected) {
            metrics.guessRejected();
            throw rejected;
        }
        metrics.guessAccepted();
        recordRoundEnd(game);
        return new GuessResult(outcome, game);
    }

    /// The won game whose score may be posted.
    ///
    /// @throws NoGameInProgressException if the player has not started a game yet
    /// @throws NoWinningGameException if the session's game was not won
    public Game requireWonGame() {
        final var game = currentGame.require();
        if (game.status() != GameStatus.WON) {
            throw new NoWinningGameException();
        }
        return game;
    }

    /// The won game whose score has not been posted yet.
    ///
    /// @throws NoGameInProgressException if the player has not started a game yet
    /// @throws NoWinningGameException if the session's game was not won
    /// @throws ScoreAlreadyPostedException if this win is already on the leaderboard
    public Game requireUnpostedWonGame() {
        final var game = requireWonGame();
        if (game.isScorePosted()) {
            throw new ScoreAlreadyPostedException();
        }
        return game;
    }

    private void recordRoundEnd(final Game game) {
        switch (game.status()) {
            case WON -> {
                metrics.gameWon(game.guessesMade());
                LOGGER.info(() -> "game.won guesses=%d".formatted(game.guessesMade()));
            }
            case LOST -> {
                metrics.gameLost();
                LOGGER.info(() -> "game.lost guesses=%d".formatted(game.guessesMade()));
            }
            case ACTIVE -> {
                // Nothing to record until the round ends.
            }
        }
    }
}
