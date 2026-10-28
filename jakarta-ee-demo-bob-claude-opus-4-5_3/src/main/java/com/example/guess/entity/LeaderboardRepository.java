package com.example.guess.entity;

import jakarta.data.Limit;
import jakarta.data.repository.*;

import java.util.List;

/**
 * Jakarta Data repository for {@link LeaderboardEntry}.
 * FR-17: top 10 ordered by fewest guesses, ties broken by recorded_at.
 */
@Repository
public interface LeaderboardRepository extends CrudRepository<LeaderboardEntry, Long> {

    /**
     * Returns at most 10 entries ordered by guesses_taken ASC, recorded_at ASC.
     * FR-17, FR-20.
     */
    @Find
    @OrderBy("guessesTaken")
    @OrderBy("recordedAt")
    List<LeaderboardEntry> findTop10(Limit limit);
}
