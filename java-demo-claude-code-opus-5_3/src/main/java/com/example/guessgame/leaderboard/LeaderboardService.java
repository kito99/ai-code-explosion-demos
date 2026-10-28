package com.example.guessgame.leaderboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Records wins and retrieves the ranked leaderboard.
 */
@Service
public class LeaderboardService {

    private final LeaderboardRepository repository;

    public LeaderboardService(LeaderboardRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists a win result (FR-15).
     *
     * @param playerName trimmed, validated player name
     * @param guessCount number of accepted guesses the win took
     */
    @Transactional
    public void recordWin(String playerName, int guessCount) {
        var entry = new LeaderboardEntry(playerName.strip(), guessCount, Instant.now());
        repository.save(entry);
    }

    /** Returns the top-10 entries ordered by fewest guesses, then earliest recorded (FR-17). */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTopEntries() {
        return repository.findTop10();
    }
}
