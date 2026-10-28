package com.example.guessthenumber.telemetry;

import com.example.guessthenumber.error.ErrorKind;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/// Counts the game events an operator needs to answer how often games are started, won,
/// and lost, and how many guesses winners take. Carries no player names, so the metrics
/// stay free of personally identifying information.
@Singleton
public class GameMetrics {

    private static final AttributeKey<String> ERROR_KIND = AttributeKey.stringKey("error.kind");
    private static final String UNIT_GAMES = "{game}";
    private static final String UNIT_GUESSES = "{guess}";
    private static final String UNIT_SCORES = "{score}";
    private static final String UNIT_ERRORS = "{error}";

    private final LongCounter gamesStarted;
    private final LongCounter gamesWon;
    private final LongCounter gamesLost;
    private final LongCounter guessesAccepted;
    private final LongCounter guessesRejected;
    private final LongCounter scoresRecorded;
    private final LongCounter apiErrors;
    private final LongHistogram winningGuessCounts;

    @Inject
    public GameMetrics(final Meter meter) {
        gamesStarted = counter(meter, "game.started", "Games started", UNIT_GAMES);
        gamesWon = counter(meter, "game.won", "Games that ended in a win", UNIT_GAMES);
        gamesLost = counter(meter, "game.lost", "Games that ended with the guesses used up", UNIT_GAMES);
        guessesAccepted = counter(meter, "game.guess.accepted", "Guesses that counted against the limit", UNIT_GUESSES);
        guessesRejected = counter(meter, "game.guess.rejected", "Guesses rejected as out of range", UNIT_GUESSES);
        scoresRecorded = counter(meter, "leaderboard.score.recorded", "Scores added to the leaderboard", UNIT_SCORES);
        apiErrors = counter(meter, "api.error", "API requests that ended in an error response", UNIT_ERRORS);
        winningGuessCounts = meter.histogramBuilder("game.winning.guesses")
                .ofLongs()
                .setDescription("How many guesses a winning game took")
                .setUnit(UNIT_GUESSES)
                .build();
    }

    public void gameStarted() {
        gamesStarted.add(1);
    }

    public void gameWon(final int guessesTaken) {
        gamesWon.add(1);
        winningGuessCounts.record(guessesTaken);
    }

    public void gameLost() {
        gamesLost.add(1);
    }

    public void guessAccepted() {
        guessesAccepted.add(1);
    }

    public void guessRejected() {
        guessesRejected.add(1);
    }

    public void scoreRecorded() {
        scoresRecorded.add(1);
    }

    public void apiError(final ErrorKind kind) {
        apiErrors.add(1, Attributes.of(ERROR_KIND, kind.name()));
    }

    private static LongCounter counter(final Meter meter, final String name, final String description,
                                       final String unit) {
        return meter.counterBuilder(name).setDescription(description).setUnit(unit).build();
    }
}
