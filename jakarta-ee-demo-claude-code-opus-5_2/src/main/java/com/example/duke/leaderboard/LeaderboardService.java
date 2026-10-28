package com.example.duke.leaderboard;

import com.example.duke.telemetry.GameTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.data.Limit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;

/// Posts wins to the leaderboard and reads the best of them back. The leaderboard is shared
/// by every player on the server, so a read reflects whatever has been posted by the time it
/// runs.
@ApplicationScoped
public class LeaderboardService {

    /// How many entries the leaderboard screen shows.
    public static final int TOP_ENTRY_COUNT = 10;

    private static final Logger LOG = Logger.getLogger(LeaderboardService.class.getName());

    @Inject
    private LeaderboardEntries entries;

    @Inject
    private GameTelemetry telemetry;

    private final Clock clock = Clock.systemUTC();

    @Transactional
    @WithSpan("leaderboard.record")
    public LeaderboardEntry record(final PlayerName playerName, final int guessCount) {
        final var saved = entries.add(new LeaderboardEntry(playerName, guessCount, clock.instant()));
        telemetry.scoreRecorded();
        // The name is deliberately left out: it belongs on the leaderboard and nowhere else.
        LOG.info(() -> format("Recorded a leaderboard entry for a win in %d guesses", guessCount));
        return saved;
    }

    @WithSpan("leaderboard.read")
    public List<LeaderboardEntry> bestResults() {
        return entries.bestResults(Limit.of(TOP_ENTRY_COUNT));
    }
}
