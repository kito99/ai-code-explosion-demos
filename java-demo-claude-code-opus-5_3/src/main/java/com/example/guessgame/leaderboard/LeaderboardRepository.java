package com.example.guessgame.leaderboard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Persistence layer for leaderboard entries.
 * Top 10 ordered by fewest guesses, then earliest recorded (FR-17).
 */
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    @Query("""
            SELECT e FROM LeaderboardEntry e
            ORDER BY e.guessCount ASC, e.recordedAt ASC
            LIMIT 10
            """)
    List<LeaderboardEntry> findTop10();
}
