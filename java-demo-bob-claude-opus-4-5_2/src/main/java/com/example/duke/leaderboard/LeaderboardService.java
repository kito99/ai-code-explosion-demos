package com.example.duke.leaderboard;

import com.example.duke.game.GameNotFoundException;
import com.example.duke.game.GameResult;
import com.example.duke.game.GameService;
import com.example.duke.game.GameStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Handles leaderboard reads and writes.
 * A score can only be posted for a WON game, and only once per gameId.
 */
@Service
public class LeaderboardService {

    private final LeaderboardRepository repository;
    private final GameService gameService;

    /** Tracks gameIds that have already had a score posted, preventing duplicates. */
    private final Set<String> postedGameIds = ConcurrentHashMap.newKeySet();

    public LeaderboardService(LeaderboardRepository repository, GameService gameService) {
        this.repository = repository;
        this.gameService = gameService;
    }

    /**
     * Returns the top 10 leaderboard entries ordered by fewest guesses, then earliest recorded.
     */
    @Observed(name = "leaderboard.list", contextualName = "LeaderboardService#listTop10")
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> listTop10() {
        var entries = repository.findTop10();
        return IntStream.range(0, entries.size())
                .mapToObj(i -> {
                    var e = entries.get(i);
                    return new LeaderboardEntryDto(i + 1, e.getPlayerName(), e.getGuessCount(), e.getRecordedAt());
                })
                .toList();
    }

    /**
     * Posts a score for a won game. Each gameId may only be posted once.
     *
     * @throws GameNotFoundException if the game ID is unknown
     * @throws InvalidScoreException if the game has not been won, or was already posted
     * @throws InvalidPlayerNameException if the name fails FR-14 validation
     */
    @Observed(name = "leaderboard.post", contextualName = "LeaderboardService#postScore")
    @Transactional
    public LeaderboardEntryDto postScore(String gameId, String rawPlayerName) {
        var trimmedName = rawPlayerName == null ? "" : rawPlayerName.strip();

        if (trimmedName.isEmpty() || trimmedName.length() > 30) {
            throw new InvalidPlayerNameException("Name must be 1–30 visible characters.");
        }

        GameResult result = gameService.findGameResult(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (result.status() != GameStatus.WON) {
            throw new InvalidScoreException("Only won games can be posted to the leaderboard.");
        }

        if (!postedGameIds.add(gameId)) {
            throw new InvalidScoreException("Score for this game has already been posted.");
        }

        var entry = new LeaderboardEntry(trimmedName, result.guessesUsed(), Instant.now());
        var saved = repository.save(entry);
        return new LeaderboardEntryDto(0, saved.getPlayerName(), saved.getGuessCount(), saved.getRecordedAt());
    }
}
