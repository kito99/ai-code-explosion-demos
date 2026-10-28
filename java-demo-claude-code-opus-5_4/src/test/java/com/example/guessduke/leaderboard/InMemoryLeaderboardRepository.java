package com.example.guessduke.leaderboard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/// Stand-in for the database so service tests can run without one. Applies the
/// same ordering the real repository asks PostgreSQL for.
public class InMemoryLeaderboardRepository implements LeaderboardRepository {

    private static final Comparator<LeaderboardEntry> BEST_FIRST =
            Comparator.comparingInt(LeaderboardEntry::guessCount)
                    .thenComparing(LeaderboardEntry::recordedAt)
                    .thenComparingLong(LeaderboardEntry::id);

    private final List<LeaderboardEntry> entries = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public LeaderboardEntry record(final PlayerName playerName, final int guessCount) {
        final var entry = new LeaderboardEntry(nextId.getAndIncrement(), playerName.value(), guessCount, Instant.now());
        entries.add(entry);
        return entry;
    }

    @Override
    public List<LeaderboardEntry> findBest(final int limit) {
        return entries.stream().sorted(BEST_FIRST).limit(limit).toList();
    }
}
