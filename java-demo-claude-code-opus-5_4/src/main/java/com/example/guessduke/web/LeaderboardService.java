package com.example.guessduke.web;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.example.guessduke.leaderboard.LeaderboardRepository;

import static com.example.guessduke.leaderboard.LeaderboardRules.VISIBLE_ENTRIES;

/// Reads the leaderboard as the screen shows it, ranked from the top down.
@Service
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardService(final LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = Objects.requireNonNull(leaderboardRepository, "leaderboardRepository");
    }

    /// Returns the best rounds posted so far, ranked from 1, never more than the
    /// screen shows, and empty when nothing has been posted.
    public List<LeaderboardRowView> bestRounds() {
        final var entries = leaderboardRepository.findBest(VISIBLE_ENTRIES);
        return IntStream.range(0, entries.size())
                .mapToObj(position -> new LeaderboardRowView(
                        position + 1,
                        entries.get(position).playerName(),
                        entries.get(position).guessCount()))
                .toList();
    }
}
