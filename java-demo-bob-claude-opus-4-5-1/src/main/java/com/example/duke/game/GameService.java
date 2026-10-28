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
 * Core game logic. Game sessions are held in memory and do not survive a restart,
 * consistent with the BRD assumption that an in-progress game is not persisted.
 * Active sessions are evicted to {@code completedResults} when a game reaches a
 * terminal state (WON or LOST), preventing unbounded map growth while still
 * allowing the leaderboard to look up a won game's result.
 */
@Service
public class GameService {

    /** Active (in-progress) sessions. Evicted to completedResults on terminal transition. */
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

    /** Test constructor accepting an injectable random for deterministic behaviour. */
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

    /** Starts a new game, returning initial state. */
    @Observed(name = "game.start", contextualName = "GameService#startGame")
    public Game startGame() {
        var gameId = UUID.randomUUID().toString();
        // nextInt(range) + MIN gives a uniform draw over [MIN, MAX] inclusive.
        var secretNumber = random.nextInt(Game.MAX_NUMBER - Game.MIN_NUMBER + 1) + Game.MIN_NUMBER;
        var session = new GameSession(gameId, secretNumber);
        sessions.put(gameId, session);
        gamesStartedCounter.increment();
        return session.toGame();
    }

    /**
     * Processes a guess for the given game session.
     * The check-and-act is synchronized on the session to prevent concurrent guesses
     * from both passing the ACTIVE guard and producing an inconsistent state.
     *
     * @throws GameNotFoundException if no session matches {@code gameId}
     * @throws GameAlreadyEndedException if the game is already won or lost
     */
    @Observed(name = "game.guess", contextualName = "GameService#guess")
    public Game guess(String gameId, int guessValue) {
        var session = sessions.get(gameId);

        // If the game is not in the active map, check whether it has already ended.
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
     * Returns a lightweight result snapshot usable by the leaderboard.
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
        completedResults.put(gameId, session.toResult());
        sessions.remove(gameId);
    }
}
