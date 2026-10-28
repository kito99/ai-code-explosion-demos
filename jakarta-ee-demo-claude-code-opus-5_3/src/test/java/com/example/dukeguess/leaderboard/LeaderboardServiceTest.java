package com.example.dukeguess.leaderboard;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.IntStream;

import static com.example.dukeguess.leaderboard.LeaderboardService.LEADERBOARD_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

class LeaderboardServiceTest {

    private static final Instant RECORDED_AT = Instant.parse("2026-10-28T12:00:00Z");

    private final InMemoryLeaderboardEntries leaderboardEntries = new InMemoryLeaderboardEntries();
    private final LeaderboardService leaderboardService =
            new LeaderboardService(leaderboardEntries, Clock.fixed(RECORDED_AT, ZoneOffset.UTC));

    @Test
    void storesTheTrimmedNameGuessCountAndTimeOfTheWin() {
        leaderboardService.postScore(PlayerName.of("  Duke  "), 3);

        assertThat(leaderboardEntries.added()).singleElement().satisfies(entry -> {
            assertThat(entry.getPlayerName()).isEqualTo("Duke");
            assertThat(entry.getGuessCount()).isEqualTo(3);
            assertThat(entry.getRecordedAt()).isEqualTo(RECORDED_AT);
        });
    }

    @Test
    void keepsEveryWinEvenUnderTheSameName() {
        leaderboardService.postScore(PlayerName.of("Duke"), 2);
        leaderboardService.postScore(PlayerName.of("Duke"), 4);

        assertThat(leaderboardEntries.added()).hasSize(2);
    }

    @Test
    void ranksFewestGuessesFirstAndNumbersRowsFromOne() {
        leaderboardService.postScore(PlayerName.of("Grace"), 4);
        leaderboardService.postScore(PlayerName.of("Ada"), 1);

        assertThat(leaderboardService.bestResults()).containsExactly(
                new RankedResult(1, "Ada", 1),
                new RankedResult(2, "Grace", 4));
    }

    @Test
    void breaksTiesByWhichResultWasRecordedFirst() {
        leaderboardService.postScore(PlayerName.of("First"), 2);
        leaderboardService.postScore(PlayerName.of("Second"), 2);

        assertThat(leaderboardService.bestResults())
                .extracting(RankedResult::playerName)
                .containsExactly("First", "Second");
    }

    @Test
    void showsNoMoreThanTheTopTen() {
        IntStream.rangeClosed(1, LEADERBOARD_SIZE + 5)
                .forEach(entry -> leaderboardService.postScore(PlayerName.of("Player " + entry), 3));

        assertThat(leaderboardService.bestResults()).hasSize(LEADERBOARD_SIZE);
    }

    @Test
    void reportsAnEmptyLeaderboardWhenNothingIsRecorded() {
        assertThat(leaderboardService.bestResults()).isEmpty();
    }
}
