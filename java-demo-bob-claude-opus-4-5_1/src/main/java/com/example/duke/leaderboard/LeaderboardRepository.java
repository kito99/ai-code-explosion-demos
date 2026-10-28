package com.example.duke.leaderboard;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Leaderboard persistence. */
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    /**
     * Returns the top 10 entries ordered by fewest guesses, then earliest recorded.
     * FR-17: top 10, fewest guesses first, ties broken by recorded-at ascending.
     */
    default List<LeaderboardEntry> findTop10() {
        var sort = Sort.by(Sort.Order.asc("guessCount"), Sort.Order.asc("recordedAt"));
        return findAll(PageRequest.of(0, 10, sort)).getContent();
    }
}
