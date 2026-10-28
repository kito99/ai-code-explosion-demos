package com.example.dukeguess.leaderboard;

import java.time.Clock;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dukeguess.telemetry.GameTelemetry;
import com.example.dukeguess.telemetry.TracedOperation;

/// Records wins and reads back the standings.
@Service
public class LeaderboardService {

    public static final int STANDINGS_SIZE = 10;

    private final LeaderboardRepository leaderboardRepository;
    private final GameTelemetry gameTelemetry;
    private final Clock clock;

    public LeaderboardService(final LeaderboardRepository leaderboardRepository,
                              final GameTelemetry gameTelemetry,
                              final Clock clock) {
        this.leaderboardRepository = leaderboardRepository;
        this.gameTelemetry = gameTelemetry;
        this.clock = clock;
    }

    @Transactional
    public LeaderboardEntry record(final PlayerName name, final int guesses) {
        final var entry = new LeaderboardEntry(name, guesses, clock.instant());
        leaderboardRepository.insert(entry);
        return entry;
    }

    /// The best [#STANDINGS_SIZE] results as they stand right now, ranked from one.
    @Transactional(readOnly = true)
    public List<LeaderboardStanding> standings() {
        return gameTelemetry.observe(TracedOperation.READ_LEADERBOARD, () -> {
            final var best = leaderboardRepository.findBest(STANDINGS_SIZE);
            return IntStream.range(0, best.size())
                    .mapToObj(position -> toStanding(position, best.get(position)))
                    .toList();
        });
    }

    private static LeaderboardStanding toStanding(final int position, final LeaderboardEntry entry) {
        return new LeaderboardStanding(position + 1, entry.name(), entry.guesses());
    }
}
