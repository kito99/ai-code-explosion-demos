package com.example.dukeguess.leaderboard;

import java.time.Instant;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.example.dukeguess.support.DukeGuessIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/// Exercises the leaderboard against a real PostgreSQL instance, where the ordering and
/// the column types actually apply.
@DukeGuessIntegrationTest
class LeaderboardServiceIntegrationTest {

    private static final Instant NOON = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clearLeaderboard() {
        jdbcClient.sql("TRUNCATE leaderboard_entry").update();
    }

    private void record(final String name, final int guesses, final Instant recordedAt) {
        leaderboardRepository.insert(new LeaderboardEntry(new PlayerName(name), guesses, recordedAt));
    }

    @Test
    void saysNothingIsPostedWhenTheTableIsEmpty() {
        assertThat(leaderboardService.standings()).isEmpty();
    }

    @Test
    void keepsEveryWinRatherThanOverwritingAName() {
        record("Duke", 2, NOON);
        record("Duke", 4, NOON.plusSeconds(60));

        assertThat(leaderboardService.standings()).hasSize(2);
    }

    @Test
    void ranksFewestGuessesFirst() {
        record("Four", 4, NOON);
        record("One", 1, NOON.plusSeconds(60));
        record("Three", 3, NOON.plusSeconds(120));

        assertThat(leaderboardService.standings())
                .extracting(standing -> standing.name().value())
                .containsExactly("One", "Three", "Four");
    }

    @Test
    void breaksATieByWhichWasRecordedFirst() {
        record("Earlier", 2, NOON);
        record("Later", 2, NOON.plusSeconds(60));

        assertThat(leaderboardService.standings())
                .extracting(standing -> standing.name().value())
                .containsExactly("Earlier", "Later");
    }

    @Test
    void numbersTheStandingsFromOne() {
        record("First", 1, NOON);
        record("Second", 2, NOON);

        assertThat(leaderboardService.standings())
                .extracting(LeaderboardStanding::rank)
                .containsExactly(1, 2);
    }

    @Test
    void showsNoMoreThanTenResults() {
        IntStream.rangeClosed(1, 12)
                .forEach(entry -> record("Player " + entry, 1, NOON.plusSeconds(entry)));

        assertThat(leaderboardService.standings()).hasSize(LeaderboardService.STANDINGS_SIZE);
    }

    @Test
    void stampsAnEntryWithTheTimeItWasRecorded() {
        final var recorded = leaderboardService.record(new PlayerName("Duke"), 3);

        assertThat(recorded.recordedAt()).isNotNull();
        assertThat(recorded.guesses()).isEqualTo(3);
    }

    @Test
    void storesWhatComesBackOutOfTheDatabaseUnchanged() {
        record("Ünicode Duke", 5, NOON);

        assertThat(leaderboardService.standings())
                .singleElement()
                .satisfies(standing -> {
                    assertThat(standing.name().value()).isEqualTo("Ünicode Duke");
                    assertThat(standing.guesses()).isEqualTo(5);
                });
    }
}
