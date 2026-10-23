package tech.virtua.duke;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/// Holds one [GuessingGame] per player so the secret number never leaves the
/// server. Games are keyed by session id and dropped once a player has been
/// away long enough.
public final class GameSessions {

    /// How long a game survives without a request before it is swept.
    public static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

    /// Upper bound on tracked games, so a flood of cookie-less requests cannot
    /// grow the map without limit.
    public static final int SESSION_LIMIT = 10_000;

    private final Map<String, TrackedGame> gamesBySessionId = new ConcurrentHashMap<>();

    /// Counted separately from the map, because a slot has to be claimed before
    /// the entry exists for the limit to hold under concurrent requests.
    private final AtomicInteger claimedSlots = new AtomicInteger();

    private final SecretNumberPicker secretNumberPicker;
    private final Clock clock;

    public GameSessions(final SecretNumberPicker secretNumberPicker, final Clock clock) {
        this.secretNumberPicker = requireNonNull(secretNumberPicker);
        this.clock = requireNonNull(clock);
    }

    /// The game for `sessionId`, starting one if this is the player's first
    /// request. Marks the session as seen just now.
    ///
    /// @throws TooManySessionsException when no slot is free for a new session
    public GuessingGame gameFor(final String sessionId) {
        requireNonNull(sessionId);
        final var existing = gamesBySessionId.get(sessionId);
        if (existing != null) {
            return existing.touch(nowEpochMilli());
        }
        return startGameFor(sessionId);
    }

    /// Drops every game whose player has been away longer than [#IDLE_TIMEOUT],
    /// and returns how many were dropped.
    public int removeIdleGames() {
        final var cutoff = nowEpochMilli() - IDLE_TIMEOUT.toMillis();
        final var idle = gamesBySessionId.entrySet().stream()
                .filter(session -> session.getValue().lastSeenEpochMilli() < cutoff)
                .toList();
        var removed = 0;
        for (final var session : idle) {
            if (releaseIfStillPresent(session.getKey(), session.getValue())) {
                removed++;
            }
        }
        return removed;
    }

    /// How many games are currently tracked.
    public int trackedGameCount() {
        return gamesBySessionId.size();
    }

    private GuessingGame startGameFor(final String sessionId) {
        claimSlotOrReclaimOne();
        final var started = new TrackedGame(new GuessingGame(secretNumberPicker), nowEpochMilli());
        final var alreadyStarted = gamesBySessionId.putIfAbsent(sessionId, started);
        if (alreadyStarted != null) {
            claimedSlots.decrementAndGet();
            return alreadyStarted.touch(nowEpochMilli());
        }
        return started.touch(nowEpochMilli());
    }

    private void claimSlotOrReclaimOne() {
        if (claimSlot()) {
            return;
        }
        removeIdleGames();
        if (!claimSlot()) {
            throw new TooManySessionsException(SESSION_LIMIT);
        }
    }

    private boolean claimSlot() {
        int claimed;
        do {
            claimed = claimedSlots.get();
            if (claimed >= SESSION_LIMIT) {
                return false;
            }
        } while (!claimedSlots.compareAndSet(claimed, claimed + 1));
        return true;
    }

    private boolean releaseIfStillPresent(final String sessionId, final TrackedGame idle) {
        if (!gamesBySessionId.remove(sessionId, idle)) {
            return false;
        }
        claimedSlots.decrementAndGet();
        return true;
    }

    private long nowEpochMilli() {
        return clock.millis();
    }

    /// A game plus the last time its player was seen. Not a record: the
    /// timestamp is deliberately mutable, so value semantics would mislead.
    private static final class TrackedGame {

        private final GuessingGame game;
        private final AtomicLong lastSeenEpochMilli;

        private TrackedGame(final GuessingGame game, final long lastSeenEpochMilli) {
            this.game = requireNonNull(game);
            this.lastSeenEpochMilli = new AtomicLong(lastSeenEpochMilli);
        }

        private GuessingGame touch(final long nowEpochMilli) {
            lastSeenEpochMilli.set(nowEpochMilli);
            return game;
        }

        private long lastSeenEpochMilli() {
            return lastSeenEpochMilli.get();
        }
    }
}
