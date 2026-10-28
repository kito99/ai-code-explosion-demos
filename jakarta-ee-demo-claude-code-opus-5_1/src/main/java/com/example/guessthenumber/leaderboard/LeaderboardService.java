package com.example.guessthenumber.leaderboard;

import com.example.guessthenumber.game.GameService;
import com.example.guessthenumber.telemetry.GameMetrics;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/// Posts wins to the leaderboard and reads the best results back. The guess count comes
/// from the session's own game rather than from the request, so a caller cannot claim a
/// better score than it played.
@Singleton
public class LeaderboardService {

    /// How many entries the leaderboard screen shows.
    public static final int TOP_ENTRY_LIMIT = 10;

    private static final Logger LOGGER = Logger.getLogger(LeaderboardService.class.getName());

    private final LeaderboardRepository repository;
    private final GameService gameService;
    private final GameMetrics metrics;

    @Inject
    public LeaderboardService(final LeaderboardRepository repository, final GameService gameService,
                              final GameMetrics metrics) {
        this.repository = requireNonNull(repository, "repository");
        this.gameService = requireNonNull(gameService, "gameService");
        this.metrics = requireNonNull(metrics, "metrics");
    }

    /// Records the session's win under the given name.
    ///
    /// @throws InvalidPlayerNameException if the name is blank or too long
    /// @throws com.example.guessthenumber.game.NoGameInProgressException if no game was started
    /// @throws com.example.guessthenumber.game.NoWinningGameException if the game was not won
    /// @throws com.example.guessthenumber.game.ScoreAlreadyPostedException if the win was already posted
    @WithSpan("leaderboard.record")
    public LeaderboardEntry recordWin(final String submittedName) {
        final var playerName = new PlayerName(submittedName);
        final var wonGame = gameService.requireUnpostedWonGame();

        final var entry = repository.add(playerName, wonGame.guessesMade(), Instant.now());
        wonGame.markScorePosted();
        metrics.scoreRecorded();
        LOGGER.info(() -> "leaderboard.recorded guesses=%d".formatted(entry.guessCount()));
        return entry;
    }

    /// The leaderboard as it stands right now, fewest guesses first.
    @WithSpan("leaderboard.read")
    public List<LeaderboardEntry> bestEntries() {
        return repository.findBest(TOP_ENTRY_LIMIT);
    }
}
