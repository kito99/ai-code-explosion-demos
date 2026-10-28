package com.example.guessgame;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    @Query("SELECT e FROM LeaderboardEntry e ORDER BY e.guessCount ASC, e.recordedAt ASC LIMIT 10")
    List<LeaderboardEntry> findTop10ByOrderByGuessCountAscRecordedAtAsc();
}
