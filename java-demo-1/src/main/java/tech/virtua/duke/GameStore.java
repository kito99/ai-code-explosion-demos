package tech.virtua.duke;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * Keeps each player's game on the server, keyed by a session id that rides along in a
 * cookie. Idle games are swept so a long-running demo does not grow without bound.
 *
 * <p>Looking a session up, deciding whether it has expired, and marking it as freshly
 * used all happen inside one {@code compute} call. That keeps two overlapping requests
 * from each creating a game for the same session, and keeps the janitor from evicting a
 * session out from under a request that just claimed it.
 */
public final class GameStore {

    private static final Duration IDLE_TIMEOUT = Duration.ofHours(2);

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final RandomGenerator random;

    public GameStore(RandomGenerator random) {
        this.random = random;
    }

    /**
     * Runs {@code action} against the caller's game and returns what it produced, along
     * with the session id the response should hand back to the browser.
     *
     * @param startFresh when true, the caller's current game is thrown away and replaced
     */
    public Result apply(String sessionId, boolean startFresh, Function<Game, GameState> action) {
        String id = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;

        Session session = sessions.compute(id, (key, existing) -> {
            if (startFresh || existing == null || existing.isExpired()) {
                return new Session(new Game(random));
            }
            existing.touch();
            return existing;
        });

        synchronized (session) {
            return new Result(id, action.apply(session.game));
        }
    }

    public void evictExpired() {
        // computeIfPresent shares the compute() lock above, so a session cannot be
        // evicted in the gap between a request claiming it and touching it.
        for (String id : sessions.keySet()) {
            sessions.computeIfPresent(id, (key, session) -> session.isExpired() ? null : session);
        }
    }

    int size() {
        return sessions.size();
    }

    public record Result(String sessionId, GameState state) {
    }

    private static final class Session {
        private final Game game;
        private Instant lastSeen = Instant.now();

        private Session(Game game) {
            this.game = game;
        }

        private void touch() {
            lastSeen = Instant.now();
        }

        private boolean isExpired() {
            return lastSeen.plus(IDLE_TIMEOUT).isBefore(Instant.now());
        }
    }
}
