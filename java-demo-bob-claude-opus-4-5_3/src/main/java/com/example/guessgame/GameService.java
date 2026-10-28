package com.example.guessgame;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active game sessions (in memory) and leaderboard persistence.
 * Sessions live for the lifetime of the server process; there is no eviction
 * policy, which is acceptable for a single-user demo.
 */
@Service
public class GameService {

    private final Map<String, Game> sessions = new ConcurrentHashMap<>();
    /** Sessions for which a leaderboard score has already been recorded. */
    private final Set<String> scoredSessions = ConcurrentHashMap.newKeySet();
    private final LeaderboardRepository leaderboardRepository;

    public GameService(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    /** Creates a new game session and returns its ID. */
    public String startGame() {
        var sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new Game());
        return sessionId;
    }

    /**
     * Submits a guess for the given session.
     *
     * @throws GameNotFoundException if the session does not exist
     */
    public GuessResult submitGuess(String sessionId, int number) {
        var game = requireGame(sessionId);
        return game.guess(number);
    }

    /**
     * Returns the game for the given session.
     *
     * @throws GameNotFoundException if the session does not exist
     */
    public Game getGame(String sessionId) {
        return requireGame(sessionId);
    }

    /**
     * Records a winning result on the leaderboard.
     *
     * @throws GameNotFoundException        if the session does not exist
     * @throws IllegalStateException        if the game is not in a won state
     * @throws ScoreAlreadyPostedException  if a score was already posted for this session
     */
    @Transactional
    public LeaderboardEntry recordScore(String sessionId, String playerName) {
        var game = requireGame(sessionId);
        if (!game.isWon()) {
            throw new IllegalStateException("Cannot record score for a game that has not been won.");
        }
        if (!scoredSessions.add(sessionId)) {
            throw new ScoreAlreadyPostedException(sessionId);
        }
        // Strip whitespace before persisting; @Size(max=30) on the DTO is checked pre-strip,
        // but the stripped form is what gets stored.
        var name = playerName.strip();
        var entry = new LeaderboardEntry(name, game.getGuessCount(), Instant.now());
        return leaderboardRepository.save(entry);
    }

    /** Returns the top-10 leaderboard entries. */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboard() {
        return leaderboardRepository.findTop10();
    }

    private Game requireGame(String sessionId) {
        var game = sessions.get(sessionId);
        if (game == null) {
            throw new GameNotFoundException(sessionId);
        }
        return game;
    }
}
