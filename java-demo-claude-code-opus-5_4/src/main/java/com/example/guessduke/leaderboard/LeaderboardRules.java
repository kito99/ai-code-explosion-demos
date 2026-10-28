package com.example.guessduke.leaderboard;

/// The limits the leaderboard applies to the names it accepts and the rows it shows.
public final class LeaderboardRules {

    /// Longest name accepted once surrounding whitespace is trimmed.
    public static final int MAX_NAME_LENGTH = 30;

    /// Number of rows the leaderboard screen shows.
    public static final int VISIBLE_ENTRIES = 10;

    private LeaderboardRules() {
    }
}
