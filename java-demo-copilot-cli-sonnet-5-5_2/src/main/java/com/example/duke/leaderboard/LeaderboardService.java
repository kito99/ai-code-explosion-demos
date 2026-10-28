package com.example.duke.leaderboard;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.example.duke.game.GameService;

@Service
public class LeaderboardService {

    public static final int MAX_NAME_LENGTH = 30;

    private final ScoreRepository scoreRepository;
    private final GameService gameService;
    private final Clock clock;

    public LeaderboardService(final ScoreRepository scoreRepository, final GameService gameService,
            final Clock clock) {
        this.scoreRepository = scoreRepository;
        this.gameService = gameService;
        this.clock = clock;
    }

    public List<LeaderboardEntry> topEntries() {
        final var scores = scoreRepository.findTop10ByOrderByGuessesAscRecordedAtAscIdAsc();
        return IntStream.range(0, scores.size())
                .mapToObj(index -> toEntry(index + 1, scores.get(index)))
                .toList();
    }

    /** Records the win of the given game under the given name; the name is validated first. */
    public Score postScore(final String gameId, final String rawName) {
        final var name = validatedName(rawName);
        final var game = gameService.findGame(gameId);
        return game.recordWin(guesses -> scoreRepository.save(
                new Score(name, guesses, clock.instant().truncatedTo(ChronoUnit.MICROS))));
    }

    static String validatedName(final String rawName) {
        final var name = rawName == null ? "" : rawName.strip();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
            throw new InvalidNameException();
        }
        return name;
    }

    private static LeaderboardEntry toEntry(final int rank, final Score score) {
        return new LeaderboardEntry(rank, score.getName(), score.getGuesses(), score.getRecordedAt());
    }
}
