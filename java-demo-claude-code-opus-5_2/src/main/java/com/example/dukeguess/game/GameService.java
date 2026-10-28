package com.example.dukeguess.game;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.dukeguess.leaderboard.LeaderboardEntry;
import com.example.dukeguess.leaderboard.LeaderboardService;
import com.example.dukeguess.leaderboard.PlayerName;
import com.example.dukeguess.telemetry.GameMetrics;
import com.example.dukeguess.telemetry.GameTelemetry;
import com.example.dukeguess.telemetry.TelemetryTag;
import com.example.dukeguess.telemetry.TracedOperation;

import static com.example.dukeguess.telemetry.GameTelemetry.tag;

/// Drives a round from the server side: it owns the secret number, scores every guess,
/// and decides whether a win may be posted to the leaderboard.
///
/// Logs here carry game ids, guess counts, and outcomes only. The player's name stays out
/// of telemetry, so a trace or log line never identifies anyone.
@Service
public class GameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameService.class);

    private final GameSessions gameSessions;
    private final LeaderboardService leaderboardService;
    private final GameMetrics gameMetrics;
    private final GameTelemetry gameTelemetry;

    public GameService(final GameSessions gameSessions,
                       final LeaderboardService leaderboardService,
                       final GameMetrics gameMetrics,
                       final GameTelemetry gameTelemetry) {
        this.gameSessions = gameSessions;
        this.leaderboardService = leaderboardService;
        this.gameMetrics = gameMetrics;
        this.gameTelemetry = gameTelemetry;
    }

    public GameSnapshot startGame() {
        return gameTelemetry.observe(TracedOperation.START_GAME, () -> {
            final var game = gameSessions.start();
            gameMetrics.gameStarted();
            LOGGER.info("Started game {}", game.id());
            return game.snapshot();
        });
    }

    public GuessOutcome submitGuess(final UUID gameId, final int guess) {
        return gameTelemetry.observe(TracedOperation.SUBMIT_GUESS, observation -> {
            final var game = gameSessions.require(gameId);
            final var outcome = score(game, guess);
            tag(observation, TelemetryTag.VERDICT, outcome.verdict());
            final var finalState = outcome.game();
            if (finalState.status().isOver()) {
                tag(observation, TelemetryTag.OUTCOME, finalState.status());
                gameMetrics.gameFinished(finalState.status(), finalState.guessesMade());
                LOGGER.info("Game {} ended as {} after {} guesses",
                        gameId, finalState.status(), finalState.guessesMade());
            }
            return outcome;
        });
    }

    private GuessOutcome score(final GuessingGame game, final int guess) {
        final GuessOutcome outcome;
        try {
            outcome = game.submitGuess(guess);
        } catch (final InvalidGuessException rejected) {
            gameMetrics.guessRejected();
            throw rejected;
        }
        gameMetrics.guessAccepted(outcome.verdict());
        return outcome;
    }

    /// Writes a won round to the leaderboard under the given name.
    ///
    /// The server scores the round, so the guess count on the entry is the one it saw
    /// rather than one the browser supplied. A round yields at most one entry: the write
    /// is claimed first and the claim is handed back only if the write fails, so a retry
    /// after a database outage still records the win.
    public LeaderboardEntry postScore(final UUID gameId, final String submittedName) {
        return gameTelemetry.observe(TracedOperation.POST_SCORE, observation -> {
            final var game = gameSessions.require(gameId);
            tag(observation, TelemetryTag.OUTCOME, game.status());
            if (game.status() != GameStatus.WON) {
                throw new GameNotWonException(game.status());
            }
            final var name = PlayerName.of(submittedName);
            if (!game.claimScorePost()) {
                throw new ScoreAlreadyPostedException();
            }
            return record(game, name);
        });
    }

    private LeaderboardEntry record(final GuessingGame game, final PlayerName name) {
        try {
            final var entry = leaderboardService.record(name, game.guessesMade());
            gameMetrics.scorePosted();
            LOGGER.info("Posted the win from game {} in {} guesses", game.id(), entry.guesses());
            return entry;
        } catch (final RuntimeException writeFailed) {
            game.releaseScorePost();
            throw writeFailed;
        }
    }
}
