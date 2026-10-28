package com.example.game.repository;

import com.example.game.model.LeaderboardEntry;
import jakarta.data.Sort;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

import java.util.List;

/**
 * Jakarta Data repository for {@link LeaderboardEntry}. The provider generates
 * the implementation at deploy time.
 */
@Repository
public interface LeaderboardRepository extends BasicRepository<LeaderboardEntry, Long> {

    /**
     * Returns the top 10 entries ordered by fewest guesses first, then by
     * insertion order (id ascending) to break ties consistently.
     */
    @Find
    @OrderBy("guessCount")
    @OrderBy("id")
    List<LeaderboardEntry> findTopEntries();
}
