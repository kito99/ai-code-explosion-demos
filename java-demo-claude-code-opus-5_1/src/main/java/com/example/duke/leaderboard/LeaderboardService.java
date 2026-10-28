package com.example.duke.leaderboard;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/// Reads and writes the shared leaderboard.
///
/// Player names are stored and shown, never logged, so telemetry stays free of personal data.
@Service
public class LeaderboardService {

    /// Entries the leaderboard screen shows.
    public static final int TOP_ENTRIES = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardService.class);

    /// Fewest guesses first; ties go to whoever was recorded first, with the id as a final tiebreak
    /// for entries that share a timestamp.
    private static final Sort BEST_FIRST = Sort.by(Sort.Direction.ASC, "guesses", "recordedAt", "id");

    private final LeaderboardRepository repository;
    private final Clock clock;

    public LeaderboardService(final LeaderboardRepository repository, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /// Adds a win to the leaderboard. Every win creates its own entry; nothing is overwritten.
    @Observed(name = "duke.leaderboard.record", contextualName = "record-score")
    @Transactional
    public PostedScore record(final PlayerName playerName, final int guesses) {
        requireNonNull(playerName, "playerName is required");
        final var saved = repository.save(new LeaderboardEntry(playerName, guesses, clock.instant()));
        LOGGER.info("Recorded a win of {} guesses as leaderboard entry {}", guesses, saved.getId());
        return new PostedScore(saved.getPlayerName(), saved.getGuesses(), saved.getRecordedAt());
    }

    /// Returns the best results as they stand right now, ranked from first place down.
    @Observed(name = "duke.leaderboard.top", contextualName = "read-leaderboard")
    @Transactional(readOnly = true)
    public List<LeaderboardStanding> topStandings() {
        final var entries = repository.findAllBy(PageRequest.of(0, TOP_ENTRIES, BEST_FIRST));
        return IntStream.range(0, entries.size())
                .mapToObj(index -> toStanding(index + 1, entries.get(index)))
                .toList();
    }

    private static LeaderboardStanding toStanding(final int rank, final LeaderboardEntry entry) {
        return new LeaderboardStanding(rank, entry.getPlayerName(), entry.getGuesses(), entry.getRecordedAt());
    }
}
