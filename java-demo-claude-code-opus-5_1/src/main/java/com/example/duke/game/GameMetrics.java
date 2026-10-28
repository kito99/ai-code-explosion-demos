package com.example.duke.game;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;

/// Publishes the game-lifecycle meters: how often games start, how they end, and how guesses land.
/// Nothing here carries a player name, so the metrics stay free of personal data.
@Component
public class GameMetrics {

    private static final String OUTCOME_TAG = "outcome";

    private final MeterRegistry registry;
    private final DistributionSummary guessesPerWin;

    public GameMetrics(final MeterRegistry registry, final GameStore store) {
        this.registry = registry;
        this.guessesPerWin = DistributionSummary.builder("duke.wins.guesses")
                .description("Guesses it took to win a game")
                .baseUnit("guesses")
                .publishPercentiles(0.5, 0.95)
                .register(registry);
        registry.gauge("duke.games.active", store, GameStore::activeCount);
    }

    /// Counts a game that has just begun.
    public void gameStarted() {
        registry.counter("duke.games.started").increment();
    }

    /// Counts a finished game and, for a win, records how many guesses it took.
    public void gameFinished(final GameStatus status, final int guessesMade) {
        registry.counter("duke.games.finished", OUTCOME_TAG, tagValue(status)).increment();
        if (status == GameStatus.WON) {
            guessesPerWin.record(guessesMade);
        }
    }

    /// Counts a guess the game accepted.
    public void guessAccepted(final GuessOutcome outcome) {
        registry.counter("duke.guesses.accepted", OUTCOME_TAG, tagValue(outcome)).increment();
    }

    /// Counts a guess the game turned away because it was out of range.
    public void guessRejected() {
        registry.counter("duke.guesses.rejected").increment();
    }

    /// Counts a win added to the leaderboard.
    public void scorePosted() {
        registry.counter("duke.scores.posted").increment();
    }

    private static String tagValue(final Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
