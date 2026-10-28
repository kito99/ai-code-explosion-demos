package com.example.dukeguess.leaderboard;

import jakarta.data.Limit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Stand-in for the database-backed repository. Sorting is stable, so entries
/// added earlier win ties just as the database ordering does.
class InMemoryLeaderboardEntries implements LeaderboardEntries {

    private static final Comparator<LeaderboardEntry> RANKING = Comparator
            .comparingInt(LeaderboardEntry::getGuessCount)
            .thenComparing(LeaderboardEntry::getRecordedAt);

    private final List<LeaderboardEntry> entries = new ArrayList<>();

    @Override
    public LeaderboardEntry add(final LeaderboardEntry entry) {
        entries.add(entry);
        return entry;
    }

    @Override
    public List<LeaderboardEntry> bestResults(final Limit limit) {
        return entries.stream()
                .sorted(RANKING)
                .limit(limit.maxResults())
                .toList();
    }

    List<LeaderboardEntry> added() {
        return List.copyOf(entries);
    }
}
