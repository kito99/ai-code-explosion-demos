package com.example.duke.game;

import com.example.duke.config.GameProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameStoreTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);

    private final GameProperties properties = new GameProperties(SESSION_TIMEOUT, Duration.ofMinutes(5));

    @Test
    void updatesAGameInPlace() {
        final var store = storeAt(NOW);
        final var game = store.add(Game.start(UUID.randomUUID(), 7, NOW));

        final var updated = store.update(game.id(), current -> current.withGuess(3));

        assertThat(updated.guessesMade()).isEqualTo(1);
        assertThat(store.activeCount()).isEqualTo(1);
    }

    @Test
    void reportsAnUnknownGame() {
        final var store = storeAt(NOW);

        assertThatThrownBy(() -> store.update(UUID.randomUUID(), game -> game))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void dropsGamesNobodyCameBackTo() {
        final var store = storeAt(NOW);
        store.add(Game.start(UUID.randomUUID(), 7, NOW.minus(SESSION_TIMEOUT).minusSeconds(1)));
        final var fresh = store.add(Game.start(UUID.randomUUID(), 7, NOW));

        store.removeAbandonedGames();

        assertThat(store.activeCount()).isEqualTo(1);
        assertThat(store.update(fresh.id(), game -> game)).isEqualTo(fresh);
    }

    private GameStore storeAt(final Instant instant) {
        return new GameStore(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }
}
