package tech.virtua.duke;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.virtua.duke.GameSessions.IDLE_TIMEOUT;
import static tech.virtua.duke.GameSessions.SESSION_LIMIT;

class GameSessionsTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final int SECRET = 7;

    private final AtomicReference<Instant> now = new AtomicReference<>(START);
    private final GameSessions sessions = new GameSessions(() -> SECRET, movableClock());

    private Clock movableClock() {
        return new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(final ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
    }

    @Test
    void returnsTheSameGameForTheSameSession() {
        assertSame(sessions.gameFor("alice"), sessions.gameFor("alice"));
        assertEquals(1, sessions.trackedGameCount());
    }

    @Test
    void keepsPlayersGamesApart() {
        final var alice = sessions.gameFor("alice");
        final var bob = sessions.gameFor("bob");

        assertNotSame(alice, bob);

        alice.submitGuess("1");

        assertEquals(1, alice.state().guessesMade());
        assertEquals(0, bob.state().guessesMade());
        assertEquals(2, sessions.trackedGameCount());
    }

    @Test
    void forgetsGamesWhosePlayersHaveGoneAway() {
        sessions.gameFor("alice");
        now.set(START.plus(IDLE_TIMEOUT).plus(1, MINUTES));

        assertEquals(1, sessions.removeIdleGames());
        assertEquals(0, sessions.trackedGameCount());
    }

    @Test
    void keepsGamesThatAreStillBeingPlayed() {
        sessions.gameFor("alice");
        now.set(START.plus(IDLE_TIMEOUT).minus(1, MINUTES));

        assertEquals(0, sessions.removeIdleGames());
        assertEquals(1, sessions.trackedGameCount());
    }

    @Test
    void refusesANewSessionOnceEverySlotIsTaken() {
        IntStream.range(0, SESSION_LIMIT).forEach(slot -> sessions.gameFor("player-" + slot));

        assertEquals(SESSION_LIMIT, sessions.trackedGameCount());
        assertThrows(TooManySessionsException.class, () -> sessions.gameFor("one-too-many"));
    }

    @Test
    void stillServesKnownPlayersWhenEverySlotIsTaken() {
        IntStream.range(0, SESSION_LIMIT).forEach(slot -> sessions.gameFor("player-" + slot));

        assertEquals(1, sessions.gameFor("player-0").submitGuess("1").guessesMade());
    }

    @Test
    void reusesTheSlotsOfPlayersWhoHaveGoneAway() {
        IntStream.range(0, SESSION_LIMIT).forEach(slot -> sessions.gameFor("player-" + slot));
        now.set(START.plus(IDLE_TIMEOUT).plus(1, MINUTES));

        assertDoesNotThrow(() -> sessions.gameFor("a-new-player"));
        assertEquals(1, sessions.trackedGameCount());
    }

    @Test
    void neverExceedsTheLimitWhenPlayersArriveAtOnce() throws Exception {
        final var crowd = SESSION_LIMIT + 500;
        final var refused = new AtomicInteger();
        try (final var arrivals = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, crowd).forEach(slot -> arrivals.submit(() -> {
                try {
                    sessions.gameFor("player-" + slot);
                } catch (final TooManySessionsException noSlot) {
                    refused.incrementAndGet();
                }
            }));
        }

        assertEquals(SESSION_LIMIT, sessions.trackedGameCount());
        assertEquals(crowd - SESSION_LIMIT, refused.get());
    }

    @Test
    void comingBackKeepsAGameAlive() {
        final var game = sessions.gameFor("alice");
        now.set(START.plus(IDLE_TIMEOUT).minus(1, MINUTES));
        sessions.gameFor("alice");
        now.set(START.plus(IDLE_TIMEOUT).plus(1, MINUTES));

        assertEquals(0, sessions.removeIdleGames());
        assertSame(game, sessions.gameFor("alice"));
    }
}
