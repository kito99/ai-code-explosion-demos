package com.example.duke.game;

import com.example.duke.config.GameProperties;
import com.example.duke.leaderboard.LeaderboardService;
import com.example.duke.leaderboard.PlayerName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/// Runs games held in memory and records wins on the leaderboard. Emits a span and a counter for
/// each lifecycle step. Telemetry never includes player names.
@Service
public class GameService {

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();
    private final SecretNumberGenerator secretGenerator;
    private final LeaderboardService leaderboard;
    private final GameProperties properties;
    private final ObservationRegistry observations;
    private final MeterRegistry meters;
    private final Clock clock;

    public GameService(final SecretNumberGenerator secretGenerator, final LeaderboardService leaderboard,
                       final GameProperties properties, final ObservationRegistry observations,
                       final MeterRegistry meters, final Clock clock) {
        this.secretGenerator = secretGenerator;
        this.leaderboard = leaderboard;
        this.properties = properties;
        this.observations = observations;
        this.meters = meters;
        this.clock = clock;
    }

    public GameSnapshot start() {
        return observe("duke.game.start", () -> {
            discardIdleGames();
            final var game = new Game(secretGenerator.next(), clock.instant());
            games.put(game.id(), game);
            meters.counter("duke.games.started").increment();
            return game.snapshot();
        });
    }

    public GuessResult guess(final UUID gameId, final int guess) {
        return observe("duke.game.guess", () -> {
            final var result = find(gameId).guess(guess, clock.instant());
            meters.counter("duke.guesses", "outcome", result.outcome().name()).increment();
            switch (result.game().status()) {
                case WON -> meters.counter("duke.games.won").increment();
                case LOST -> meters.counter("duke.games.lost").increment();
                case IN_PROGRESS -> { }
            }
            return result;
        });
    }

    public void postScore(final UUID gameId, final PlayerName name) {
        observe("duke.game.post-score", () -> {
            final var game = find(gameId);
            final var guesses = game.claimScore(clock.instant());
            try {
                leaderboard.record(name, guesses);
            } catch (final RuntimeException failure) {
                game.releaseScore();
                throw failure;
            }
            meters.counter("duke.scores.posted").increment();
            return null;
        });
    }

    private Game find(final UUID gameId) {
        final var game = games.get(gameId);
        if (game == null) {
            throw new GameException(GameError.GAME_NOT_FOUND, "That game could not be found. Start a new game to play.");
        }
        return game;
    }

    private void discardIdleGames() {
        final var cutoff = clock.instant().minus(properties.sessionTimeout());
        games.values().removeIf(game -> game.isIdleSince(cutoff));
    }

    private <T> T observe(final String name, final java.util.function.Supplier<T> action) {
        return Observation.createNotStarted(name, observations).observe(action);
    }
}
