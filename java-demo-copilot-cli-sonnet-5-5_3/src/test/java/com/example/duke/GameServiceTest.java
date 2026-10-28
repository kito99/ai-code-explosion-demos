package com.example.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class GameServiceTest {

    private static final int SECRET = 4;

    private final InMemoryRepository repository = new InMemoryRepository();
    private final GameService service = new GameService(repository, () -> SECRET);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc", "3.5", "0", "11", "-1", "99999999999999", "1 2"})
    void invalidGuessesAreRejectedWithoutCounting(final String raw) {
        final var game = service.startGame();
        assertThrows(InvalidGuessException.class, () -> service.guess(game.id(), raw));
        assertEquals(0, game.guessesMade());
    }

    @Test
    void acceptsGuessWithSurroundingWhitespace() {
        final var game = service.startGame();
        assertEquals(GuessResult.CORRECT, service.guess(game.id(), " 4 "));
    }

    @Test
    void unknownGameIsNotFound() {
        assertThrows(GameNotFoundException.class, () -> service.guess("missing", "1"));
        assertThrows(GameNotFoundException.class, () -> service.postScore(null, "Duke"));
    }

    @Test
    void winRecordsTrimmedNameAndGuessCountOnce() {
        final var game = service.startGame();
        service.guess(game.id(), "1");
        service.guess(game.id(), "4");
        service.postScore(game.id(), "  Duke ");
        assertEquals("Duke", repository.saved.getFirst().playerName());
        assertEquals(2, repository.saved.getFirst().guesses());
        assertThrows(GameStateException.class, () -> service.postScore(game.id(), "Duke"));
        assertEquals(1, repository.saved.size());
    }

    @Test
    void invalidNameRecordsNothingAndAllowsRetry() {
        final var game = service.startGame();
        service.guess(game.id(), "4");
        assertThrows(InvalidNameException.class, () -> service.postScore(game.id(), "   "));
        assertTrue(repository.saved.isEmpty());
        service.postScore(game.id(), "Duke");
        assertEquals(1, repository.saved.size());
    }

    @Test
    void lossAndInProgressGamesCannotPostScores() {
        final var game = service.startGame();
        assertThrows(GameStateException.class, () -> service.postScore(game.id(), "Duke"));
        for (var attempt = 0; attempt < GameRules.MAX_GUESSES; attempt++) {
            service.guess(game.id(), "1");
        }
        assertThrows(GameStateException.class, () -> service.postScore(game.id(), "Duke"));
        assertTrue(repository.saved.isEmpty());
    }

    @Test
    void failedSaveAllowsRetry() {
        final var game = service.startGame();
        service.guess(game.id(), "4");
        repository.failNextSave = true;
        assertThrows(PersistenceException.class, () -> service.postScore(game.id(), "Duke"));
        service.postScore(game.id(), "Duke");
        assertEquals(1, repository.saved.size());
    }

    @Test
    void randomSecretStaysInRangeAndCoversEveryValue() {
        final var seen = new java.util.HashSet<Integer>();
        for (var draw = 0; draw < 5_000; draw++) {
            seen.add(GameService.randomSecretNumber());
        }
        assertEquals(GameRules.MAX_NUMBER - GameRules.MIN_NUMBER + 1, seen.size());
        assertTrue(seen.stream().allMatch(value -> value >= GameRules.MIN_NUMBER && value <= GameRules.MAX_NUMBER));
    }

    private static final class InMemoryRepository implements LeaderboardRepository {
        private final List<LeaderboardEntry> saved = new ArrayList<>();
        private boolean failNextSave;

        @Override
        public LeaderboardEntry save(final String playerName, final int guesses) {
            if (failNextSave) {
                failNextSave = false;
                throw new PersistenceException("boom", new RuntimeException());
            }
            final var entry = new LeaderboardEntry(saved.size() + 1, playerName, guesses, Instant.now());
            saved.add(entry);
            return entry;
        }

        @Override
        public List<LeaderboardEntry> findTop(final int limit) {
            return saved.stream().limit(limit).toList();
        }
    }
}
