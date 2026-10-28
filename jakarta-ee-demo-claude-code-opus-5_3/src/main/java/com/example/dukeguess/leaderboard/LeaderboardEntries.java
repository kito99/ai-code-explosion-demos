package com.example.dukeguess.leaderboard;

import jakarta.data.Limit;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

import java.util.List;

/// Stores and ranks leaderboard entries. Entries are only ever added.
@Repository
public interface LeaderboardEntries {

    @Insert
    LeaderboardEntry add(LeaderboardEntry entry);

    /// Entries ranked by fewest guesses, with ties broken by which was recorded first.
    @Find
    @OrderBy("guessCount")
    @OrderBy("recordedAt")
    @OrderBy("id")
    List<LeaderboardEntry> bestResults(Limit limit);
}
