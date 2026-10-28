package com.example.duke.leaderboard;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/// Stores and reads winning results.
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    /// Returns one page of entries in the order `pageable` asks for, without a count query.
    List<LeaderboardEntry> findAllBy(Pageable pageable);
}
