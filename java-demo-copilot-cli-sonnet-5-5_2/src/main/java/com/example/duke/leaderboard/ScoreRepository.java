package com.example.duke.leaderboard;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    List<Score> findTop10ByOrderByGuessesAscRecordedAtAscIdAsc();
}
