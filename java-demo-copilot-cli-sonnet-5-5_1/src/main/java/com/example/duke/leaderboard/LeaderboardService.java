package com.example.duke.leaderboard;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Service
public class LeaderboardService {

    public static final int TOP_ENTRIES = 10;

    private final LeaderboardRepository repository;
    private final Clock clock;

    public LeaderboardService(final LeaderboardRepository repository, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(final PlayerName name, final int guesses) {
        repository.insert(name.value(), guesses, clock.instant());
    }

    public List<LeaderboardStanding> topStandings() {
        return repository.findBest(TOP_ENTRIES);
    }
}
