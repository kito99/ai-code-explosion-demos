package com.example.duke;

import java.util.List;

/** Durable storage for winning results. */
public interface LeaderboardRepository {

    LeaderboardEntry save(String playerName, int guesses);

    /** Best results first: fewest guesses, then earliest recorded. */
    List<LeaderboardEntry> findTop(int limit);
}
