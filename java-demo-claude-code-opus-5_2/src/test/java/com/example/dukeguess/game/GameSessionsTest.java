package com.example.dukeguess.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.dukeguess.config.GameProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GameSessionsTest {

    private static final Duration LIFETIME = Duration.ofHours(2);
    private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");

    private final MovableClock clock = new MovableClock(START);
    private final GameSessions sessions =
            new GameSessions(() -> 4, new GameProperties(LIFETIME), clock);

    @Test
    void handsBackAGameItJustStarted() {
        final var game = sessions.start();

        assertThat(sessions.find(game.id())).contains(game);
        assertThat(sessions.inPlayCount()).isEqualTo(1);
    }

    @Test
    void reportsAnUnknownGameAsNotInPlay() {
        final var unknownId = UUID.randomUUID();

        assertThatExceptionOfType(GameNotFoundException.class)
                .isThrownBy(() -> sessions.require(unknownId));
    }

    @Test
    void sweepsGamesThatOutlivedTheirLifetimeWhenANewOneStarts() {
        final var stale = sessions.start();

        clock.advanceBy(LIFETIME.plusMinutes(1));
        final var fresh = sessions.start();

        assertThat(sessions.find(stale.id())).isEmpty();
        assertThat(sessions.find(fresh.id())).contains(fresh);
    }

    @Test
    void keepsGamesThatAreStillWithinTheirLifetime() {
        final var earlier = sessions.start();

        clock.advanceBy(LIFETIME.minusMinutes(1));
        sessions.start();

        assertThat(sessions.find(earlier.id())).contains(earlier);
        assertThat(sessions.inPlayCount()).isEqualTo(2);
    }

    /// A clock the test moves by hand, so lifetime behavior does not depend on waiting.
    private static final class MovableClock extends Clock {

        private Instant now;

        private MovableClock(final Instant now) {
            this.now = now;
        }

        private void advanceBy(final Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }
    }
}
