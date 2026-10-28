package com.example.guessgame;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LeaderboardService {

    private final LeaderboardRepository repository;

    public LeaderboardService(LeaderboardRepository repository) {
        this.repository = repository;
    }

    public void recordWin(String playerName, int guessCount) {
        String trimmed = playerName == null ? "" : playerName.strip();
        if (trimmed.isEmpty() || trimmed.length() > 30) {
            throw new IllegalArgumentException("Name must be 1–30 characters.");
        }
        repository.save(new LeaderboardEntry(trimmed, guessCount));
    }

    public List<LeaderboardEntry> getTopEntries() {
        return repository.findTop10ByOrderByGuessCountAscRecordedAtAsc();
    }
}
