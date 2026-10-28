package com.example.game.service;

import com.example.game.model.LeaderboardEntry;
import com.example.game.repository.LeaderboardRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Records winning results and retrieves the top-10 leaderboard, ordered by
 * fewest guesses then by record time (via id for tie-breaking).
 */
@ApplicationScoped
public class LeaderboardService {

    private static final int TOP_COUNT = 10;

    @Inject
    LeaderboardRepository repository;

    /**
     * Posts a winning score. Name validation is handled by Jakarta Validation
     * on the REST layer; this method trusts a pre-validated name.
     */
    @Transactional
    public void postScore(String playerName, int guessCount) {
        var entry = new LeaderboardEntry(playerName.strip(), guessCount, Instant.now());
        repository.save(entry);
    }

    /** Returns at most the top {@value #TOP_COUNT} entries. */
    public List<LeaderboardEntry> getTopEntries() {
        return repository.findTopEntries()
                .stream()
                .limit(TOP_COUNT)
                .toList();
    }
}
