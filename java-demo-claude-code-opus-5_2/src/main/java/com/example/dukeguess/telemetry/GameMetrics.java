package com.example.dukeguess.telemetry;

import java.util.Locale;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

import com.example.dukeguess.game.GameSessions;
import com.example.dukeguess.game.GameStatus;
import com.example.dukeguess.game.GuessVerdict;

/// Counts the game lifecycle so someone watching the metrics can answer how often games
/// start, how they end, and how guesses land.
@Component
public class GameMetrics {

    private static final String GAMES_STARTED = "duke.games.started";
    private static final String GAMES_FINISHED = "duke.games.finished";
    private static final String GUESSES_ACCEPTED = "duke.guesses.accepted";
    private static final String GUESSES_REJECTED = "duke.guesses.rejected";
    private static final String WINNING_GUESS_COUNT = "duke.wins.guesses";
    private static final String SCORES_POSTED = "duke.scores.posted";
    private static final String GAMES_IN_PLAY = "duke.games.in-play";

    private final MeterRegistry meterRegistry;
    private final Counter gamesStarted;
    private final Counter guessesRejected;
    private final Counter scoresPosted;
    private final DistributionSummary winningGuessCount;

    public GameMetrics(final MeterRegistry meterRegistry, final GameSessions gameSessions) {
        this.meterRegistry = meterRegistry;
        this.gamesStarted = Counter.builder(GAMES_STARTED)
                .description("Games Duke has started a number for")
                .register(meterRegistry);
        this.guessesRejected = Counter.builder(GUESSES_REJECTED)
                .description("Submissions turned away for being outside the playable range")
                .register(meterRegistry);
        this.scoresPosted = Counter.builder(SCORES_POSTED)
                .description("Wins written to the leaderboard")
                .register(meterRegistry);
        this.winningGuessCount = DistributionSummary.builder(WINNING_GUESS_COUNT)
                .description("Guesses a win took")
                .register(meterRegistry);
        Gauge.builder(GAMES_IN_PLAY, gameSessions, GameSessions::inPlayCount)
                .description("Rounds the server is currently holding")
                .register(meterRegistry);
    }

    public void gameStarted() {
        gamesStarted.increment();
    }

    public void guessAccepted(final GuessVerdict verdict) {
        meterRegistry.counter(GUESSES_ACCEPTED, TelemetryTag.VERDICT.key(), lowercase(verdict)).increment();
    }

    public void guessRejected() {
        guessesRejected.increment();
    }

    public void gameFinished(final GameStatus status, final int guessesMade) {
        meterRegistry.counter(GAMES_FINISHED, TelemetryTag.OUTCOME.key(), lowercase(status)).increment();
        if (status == GameStatus.WON) {
            winningGuessCount.record(guessesMade);
        }
    }

    public void scorePosted() {
        scoresPosted.increment();
    }

    private static String lowercase(final Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
