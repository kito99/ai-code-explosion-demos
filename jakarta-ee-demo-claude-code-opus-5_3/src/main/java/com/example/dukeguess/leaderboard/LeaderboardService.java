package com.example.dukeguess.leaderboard;

import jakarta.data.Limit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;

/// Posts winning results and reads back the ranked leaderboard.
@ApplicationScoped
public class LeaderboardService {

    public static final int LEADERBOARD_SIZE = 10;

    private final LeaderboardEntries leaderboardEntries;
    private final Clock clock;

    /// Required so the container can proxy this normal-scoped bean.
    protected LeaderboardService() {
        this(null, null);
    }

    @Inject
    public LeaderboardService(final LeaderboardEntries leaderboardEntries, final Clock clock) {
        this.leaderboardEntries = leaderboardEntries;
        this.clock = clock;
    }

    /// Records a win. Each call creates its own entry; nothing is overwritten.
    @Transactional
    public LeaderboardEntry postScore(final PlayerName playerName, final int guessCount) {
        return leaderboardEntries.add(
                new LeaderboardEntry(playerName.value(), guessCount, clock.instant()));
    }

    /// The top [#LEADERBOARD_SIZE] results, already ranked.
    public List<RankedResult> bestResults() {
        final var entries = leaderboardEntries.bestResults(Limit.of(LEADERBOARD_SIZE));
        return rank(entries);
    }

    /// Numbers entries from 1 in the order the repository returned them.
    static List<RankedResult> rank(final List<LeaderboardEntry> entries) {
        return IntStream.range(0, entries.size())
                .mapToObj(index -> RankedResult.of(index + 1, entries.get(index)))
                .toList();
    }
}
