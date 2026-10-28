package com.example.duke.leaderboard;

import jakarta.data.metamodel.StaticMetamodel;

/// Attribute names for [LeaderboardEntry], so the repository orders by compile-time
/// constants instead of loose strings.
@StaticMetamodel(LeaderboardEntry.class)
public interface _LeaderboardEntry {

    String ID = "id";
    String PLAYER_NAME = "playerName";
    String GUESS_COUNT = "guessCount";
    String RECORDED_AT = "recordedAt";
}
