package com.example.duke;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import jakarta.data.Limit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GameService {

    static final int LEADERBOARD_SIZE = 10;
    private static final int MAX_TRACKED_GAMES = 1000;

    private final RandomGenerator random;

    // Insertion-ordered so the oldest abandoned games are evicted first.
    private final Map<String, Game> games = Collections.synchronizedMap(new LinkedHashMap<>());

    @Inject
    ScoreRepository scores;

    public GameService() {
        this(new SecureRandom());
    }

    GameService(RandomGenerator random) {
        this.random = random;
    }

    public Game start() {
        Game game = new Game(random.nextInt(Game.MIN_NUMBER, Game.MAX_NUMBER + 1));
        synchronized (games) {
            games.put(game.id(), game);
            if (games.size() > MAX_TRACKED_GAMES) {
                var oldest = games.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
        }
        return game;
    }

    public Game.Hint guess(String gameId, int value) {
        return find(gameId).guess(value);
    }

    public Game find(String gameId) {
        Game game = games.get(gameId);
        if (game == null) {
            throw new GameException(404, "We couldn't find that game. Start a new game to play!");
        }
        return game;
    }

    public ScoreEntry postScore(String gameId, String name) {
        Game game = find(gameId);
        game.claimScorePosting();
        try {
            return scores.add(new ScoreEntry(name, game.guessesMade(), Instant.now()));
        } catch (RuntimeException e) {
            game.releaseScorePosting();
            throw e;
        }
    }

    public List<ScoreEntry> leaderboard() {
        return scores.best(Limit.of(LEADERBOARD_SIZE));
    }
}
