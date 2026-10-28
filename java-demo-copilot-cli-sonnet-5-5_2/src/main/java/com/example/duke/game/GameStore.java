package com.example.duke.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

/** In-memory registry of active games. Evicts the oldest game once the capacity is reached. */
@Component
public class GameStore {

    static final int MAX_ACTIVE_GAMES = 10_000;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Game> games = new LinkedHashMap<>();

    public void add(final Game game) {
        lock.lock();
        try {
            games.put(game.gameId(), game);
            if (games.size() > MAX_ACTIVE_GAMES) {
                final var eldest = games.keySet().iterator();
                eldest.next();
                eldest.remove();
            }
        } finally {
            lock.unlock();
        }
    }

    public Optional<Game> find(final String gameId) {
        lock.lock();
        try {
            return Optional.ofNullable(games.get(gameId));
        } finally {
            lock.unlock();
        }
    }
}
