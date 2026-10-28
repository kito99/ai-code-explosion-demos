package com.example.duke.game;

import com.example.duke.config.GameProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/// Holds the rounds currently in play. Games live only in memory, because an in-progress game
/// is not expected to survive a restart; only leaderboard entries are durable.
@Component
public class GameStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameStore.class);

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();
    private final GameProperties properties;
    private final Clock clock;

    public GameStore(final GameProperties properties, final Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /// Adds a freshly started game.
    public Game add(final Game game) {
        games.put(game.id(), game);
        return game;
    }

    /// Applies `change` to the stored game as a single atomic step and returns the new state.
    public Game update(final UUID gameId, final UnaryOperator<Game> change) {
        return games.compute(gameId, (_, existing) -> {
            if (existing == null) {
                throw new GameNotFoundException(gameId);
            }
            return change.apply(existing);
        });
    }

    /// Number of rounds the server is currently holding.
    public int activeCount() {
        return games.size();
    }

    /// Drops rounds nobody came back to, so abandoned games cannot pile up.
    @Scheduled(fixedDelayString = "${duke.game.cleanup-interval}")
    public void removeAbandonedGames() {
        final var cutoff = clock.instant().minus(properties.sessionTimeout());
        final var removed = games.values().removeIf(game -> game.startedAt().isBefore(cutoff));
        if (removed) {
            LOGGER.debug("Removed abandoned games, {} still active", games.size());
        }
    }
}
