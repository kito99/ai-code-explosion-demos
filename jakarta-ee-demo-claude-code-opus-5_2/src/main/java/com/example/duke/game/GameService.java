package com.example.duke.game;

import com.example.duke.leaderboard.LeaderboardEntry;
import com.example.duke.leaderboard.LeaderboardService;
import com.example.duke.leaderboard.PlayerName;
import com.example.duke.telemetry.GameTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Logger;

import static java.lang.String.format;

/// Drives the session's game: starting a round, taking guesses, and posting a win. Every
/// rule about what a guess means lives in [Game], and the session's copy of it is guarded by
/// [CurrentGame]; this class connects those to the leaderboard and the telemetry.
@ApplicationScoped
public class GameService {

    private static final Logger LOG = Logger.getLogger(GameService.class.getName());

    @Inject
    private SecretNumberGenerator secretNumbers;

    @Inject
    private CurrentGame currentGame;

    @Inject
    private LeaderboardService leaderboard;

    @Inject
    private GameTelemetry telemetry;

    /// Starts a fresh round, replacing whatever the session was playing.
    @WithSpan("game.start")
    public GameSnapshot start() {
        final var snapshot = currentGame.replaceWith(new Game(secretNumbers.draw()));
        telemetry.gameStarted();
        LOG.info("Started a new game");
        return snapshot;
    }

    /// The round the session is playing.
    ///
    /// @throws NoGameInProgressException if the session has no game
    public GameSnapshot current() {
        return currentGame.snapshot();
    }

    /// Takes a guess at the session's game and returns the round as it now stands.
    @WithSpan("game.guess")
    public GameSnapshot guess(final int value) {
        final GameSnapshot snapshot;
        try {
            snapshot = currentGame.recordGuess(value);
        } catch (final InvalidGuessException outOfRange) {
            telemetry.guessRejected();
            throw outOfRange;
        }

        telemetry.guessAccepted();
        recordOutcome(snapshot);
        return snapshot;
    }

    private void recordOutcome(final GameSnapshot snapshot) {
        switch (snapshot.status()) {
            case WON -> {
                telemetry.gameWon(snapshot.guessesMade());
                LOG.info(() -> format("Game won in %d guesses", snapshot.guessesMade()));
            }
            case LOST -> {
                telemetry.gameLost();
                LOG.info("Game lost with every guess used");
            }
            case IN_PROGRESS -> LOG.fine(() -> format("Guess %d of %d taken",
                    snapshot.guessesMade(), GameRules.MAX_GUESSES));
        }
    }

    /// Posts the session's win to the leaderboard under `playerName`. The guess count comes
    /// from the server's own record of the round, so a caller cannot claim a better score
    /// than it played, and the win is claimed before it is written so two requests arriving
    /// together cannot both record it.
    ///
    /// @throws NoGameInProgressException if the session has no game
    /// @throws NoWinningGameException if the session's game was not won
    /// @throws ScoreAlreadyPostedException if this win is already on the leaderboard
    @WithSpan("game.post.score")
    public LeaderboardEntry postScore(final PlayerName playerName) {
        final var guessCount = currentGame.claimWin();
        try {
            return leaderboard.record(playerName, guessCount);
        } catch (final RuntimeException recordingFailed) {
            // Nothing was written, so the claim goes back and the player can try again.
            currentGame.releaseClaim();
            throw recordingFailed;
        }
    }
}
