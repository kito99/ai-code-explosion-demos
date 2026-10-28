package com.example.guess.leaderboard;

import com.example.guess.entity.LeaderboardEntry;
import com.example.guess.entity.LeaderboardRepository;
import jakarta.data.Limit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Application-level service that records wins and queries the leaderboard.
 */
@ApplicationScoped
public class LeaderboardService {

    @Inject
    LeaderboardRepository repo;

    /**
     * Records a win. FR-15: each win creates its own entry.
     *
     * @param name         trimmed player name (1–30 visible characters, FR-14)
     * @param guessesTaken number of accepted guesses (1–5)
     * @return the persisted entry
     */
    @Transactional
    public LeaderboardEntry recordWin(
            @NotBlank @Size(max = 30) String name,
            @Min(1) @Max(5) int guessesTaken) {

        String trimmed = name.strip();
        LeaderboardEntry entry = new LeaderboardEntry(trimmed, guessesTaken, Instant.now());
        repo.save(entry);
        return entry;
    }

    /**
     * Returns the top 10 entries ordered by fewest guesses, then earliest recorded.
     * FR-17, FR-20.
     */
    public List<LeaderboardEntry> getTop10() {
        return repo.findTop10(Limit.of(10));
    }
}
