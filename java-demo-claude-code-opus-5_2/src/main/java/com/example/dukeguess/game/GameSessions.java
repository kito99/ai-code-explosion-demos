package com.example.dukeguess.game;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.dukeguess.config.GameProperties;

/// Holds the rounds currently in play, keyed by game id.
///
/// Rounds live in memory only: an in-progress game is not meant to survive a restart, and
/// abandoned ones are swept once they pass their lifetime so a long-running server does
/// not accumulate them.
@Component
public class GameSessions {

    private final Map<UUID, GuessingGame> gamesInPlay = new ConcurrentHashMap<>();
    private final SecretNumberPicker secretNumberPicker;
    private final GameProperties gameProperties;
    private final Clock clock;

    public GameSessions(final SecretNumberPicker secretNumberPicker,
                        final GameProperties gameProperties,
                        final Clock clock) {
        this.secretNumberPicker = secretNumberPicker;
        this.gameProperties = gameProperties;
        this.clock = clock;
    }

    public GuessingGame start() {
        sweepExpired();
        final var game = new GuessingGame(UUID.randomUUID(), secretNumberPicker.pick(), clock.instant());
        gamesInPlay.put(game.id(), game);
        return game;
    }

    public Optional<GuessingGame> find(final UUID gameId) {
        return Optional.ofNullable(gamesInPlay.get(gameId));
    }

    public GuessingGame require(final UUID gameId) {
        return find(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    public int inPlayCount() {
        return gamesInPlay.size();
    }

    private void sweepExpired() {
        final Instant cutoff = clock.instant().minus(gameProperties.sessionLifetime());
        gamesInPlay.values().removeIf(game -> game.startedAt().isBefore(cutoff));
    }
}
