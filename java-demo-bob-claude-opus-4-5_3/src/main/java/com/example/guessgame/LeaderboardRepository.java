package com.example.guessgame;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access for leaderboard entries.
 */
@Repository
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    /**
     * Returns the top 10 entries ordered by fewest guesses, ties broken by earliest recorded time.
     */
    @Query("SELECT e FROM LeaderboardEntry e ORDER BY e.guessCount ASC, e.recordedAt ASC LIMIT 10")
    List<LeaderboardEntry> findTop10();
}
