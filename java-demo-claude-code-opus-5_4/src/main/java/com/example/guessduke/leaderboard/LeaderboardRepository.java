package com.example.guessduke.leaderboard;

import java.util.List;

/// Stores winning results and reads back the best of them.
public interface LeaderboardRepository {

    /// Appends a win, leaving every existing entry untouched.
    LeaderboardEntry record(PlayerName playerName, int guessCount);

    /// Returns the best entries, fewest guesses first, ties broken by which was
    /// recorded first, capped at `limit` rows.
    List<LeaderboardEntry> findBest(int limit);
}
