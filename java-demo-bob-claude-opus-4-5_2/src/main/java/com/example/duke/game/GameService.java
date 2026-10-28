package com.example.duke.game;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core game logic. Sessions are held in memory and do not survive a restart,
 * consistent with the BRD assumption that an in-progress game is not persisted.
 * Completed sessions are moved to a separate map to free active-session memory
 * while still allowing the leaderboard to look up a won game's result.
 */
@Service
public class GameService {

    /** Active (in-progress) sessions, evicted to completedResults on terminal transition. */
    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    /** Completed game results, kept for leaderboard score posting. */
    private final ConcurrentHashMap<String, GameResult> completedResults = new ConcurrentHashMap<>();

    private final Random random;
    private final Counter gamesStartedCounter;
    private final Counter gamesWonCounter;
    private final Counter gamesLostCounter;

    @Autowired
    public GameService(MeterRegistry meterRegistry) {
        this(meterRegistry, new SecureRandom());
    }

    /** Test constructor — accepts an injectable random for deterministic behaviour. */
    public GameService(MeterRegistry meterRegistry, Random random) {
        this.random = random;
        this.gamesStartedCounter = Counter.builder("game.started")
                .description("Total games started")
                .register(meterRegistry);
        this.gamesWonCounter = Counter.builder("game.won")
                .description("Total games won")
                .register(meterRegistry);
        this.gamesLostCounter = Counter.builder("game.lost")
                .description("Total games lost")
                .register(meterRegistry);
    }

    /** Starts a new game and returns initial state. */
    @Observed(name = "game.start", contextualName = "GameService#startGame")
    public Game startGame() {
        var gameId = UUID.randomUUID().toString();
        var secretNumber = random.nextInt(Game.MAX_NUMBER - Game.MIN_NUMBER + 1) + Game.MIN_NUMBER;
        var session = new GameSession(gameId, secretNumber);
        sessions.put(gameId, session);
        gamesStartedCounter.increment();
        return session.toGame();
    }

    /**
     * Processes a guess for the given session.
     * Synchronized on the session to prevent concurrent guesses producing inconsistent state.
     *
     * @throws GameNotFoundException if no session matches {@code gameId}
     * @throws GameAlreadyEndedException if the game is already won or lost
     */
    @Observed(name = "game.guess", contextualName = "GameService#guess")
    public Game guess(String gameId, int guessValue) {
        var session = sessions.get(gameId);

        if (session == null) {
            if (completedResults.containsKey(gameId)) {
                throw new GameAlreadyEndedException(gameId);
            }
            throw new GameNotFoundException(gameId);
        }

        synchronized (session) {
            if (session.status() != GameStatus.ACTIVE) {
                throw new GameAlreadyEndedException(gameId);
            }

            session.incrementGuesses();

            if (guessValue == session.secretNumber()) {
                session.setStatus(GameStatus.WON);
                session.setLastFeedback("Correct! 🎉 You guessed it in %d %s.".formatted(
                        session.guessesUsed(),
                        session.guessesUsed() == 1 ? "guess" : "guesses"));
                gamesWonCounter.increment();
                evict(gameId, session);
            } else if (session.guessesUsed() >= Game.MAX_GUESSES) {
                session.setStatus(GameStatus.LOST);
                session.setLastFeedback("No more guesses! The number was %d.".formatted(session.secretNumber()));
                gamesLostCounter.increment();
                evict(gameId, session);
            } else if (guessValue < session.secretNumber()) {
                session.setLastFeedback("Too low! Try a higher number.");
            } else {
                session.setLastFeedback("Too high! Try a lower number.");
            }

            return session.toGame();
        }
    }

    /**
     * Returns a lightweight result snapshot for the leaderboard.
     * Works for both in-progress and completed sessions.
     */
    public Optional<GameResult> findGameResult(String gameId) {
        var active = sessions.get(gameId);
        if (active != null) {
            return Optional.of(active.toResult());
        }
        return Optional.ofNullable(completedResults.get(gameId));
    }

    private void evict(String gameId, GameSession session) {
        // Remove from active map first so concurrent findGameResult() callers see the
        // terminal state from completedResults rather than stale data from sessions.
        sessions.remove(gameId);
        completedResults.put(gameId, session.toResult());
    }
}
