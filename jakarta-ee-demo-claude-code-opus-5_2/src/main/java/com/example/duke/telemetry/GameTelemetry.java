package com.example.duke.telemetry;

import com.example.duke.error.ErrorKind;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/// The application's own OpenTelemetry instruments, which sit alongside the request spans and
/// durations the runtime records for every API call.
///
/// Nothing here carries a player name or anything else that identifies a person.
@ApplicationScoped
public class GameTelemetry {

    private static final String GUESS_UNIT = "{guess}";
    private static final String GAME_UNIT = "{game}";
    private static final String SCORE_UNIT = "{score}";
    private static final String ERROR_UNIT = "{error}";

    /// Labels an error count with the kind of failure rather than any request detail, so the
    /// counts stay free of anything that identifies a player.
    private static final AttributeKey<String> ERROR_KIND = AttributeKey.stringKey("error.kind");

    @Inject
    private Meter meter;

    private LongCounter gamesStarted;
    private LongCounter gamesWon;
    private LongCounter gamesLost;
    private LongCounter guessesAccepted;
    private LongCounter guessesRejected;
    private LongCounter scoresRecorded;
    private LongCounter apiErrors;
    private LongHistogram winningGuessCount;

    @PostConstruct
    void createInstruments() {
        gamesStarted = counter("game.started", "Games started", GAME_UNIT);
        gamesWon = counter("game.won", "Games that ended in a win", GAME_UNIT);
        gamesLost = counter("game.lost", "Games that ended with the guesses used up", GAME_UNIT);
        guessesAccepted = counter("game.guess.accepted", "Guesses that counted against the limit", GUESS_UNIT);
        guessesRejected = counter("game.guess.rejected", "Guesses rejected before they counted", GUESS_UNIT);
        scoresRecorded = counter("leaderboard.score.recorded", "Wins posted to the leaderboard", SCORE_UNIT);
        apiErrors = counter("api.error", "API requests that ended in an error", ERROR_UNIT);
        winningGuessCount = meter.histogramBuilder("game.winning.guesses")
                .ofLongs()
                .setDescription("How many guesses a win took")
                .setUnit(GUESS_UNIT)
                .build();
    }

    private LongCounter counter(final String name, final String description, final String unit) {
        return meter.counterBuilder(name).setDescription(description).setUnit(unit).build();
    }

    public void gameStarted() {
        gamesStarted.add(1);
    }

    public void gameWon(final int guessCount) {
        gamesWon.add(1);
        winningGuessCount.record(guessCount);
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
}
