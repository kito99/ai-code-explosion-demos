package com.example.guessduke.leaderboard;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.example.guessduke.PostgresTestcontainerConfiguration;

import static com.example.guessduke.leaderboard.LeaderboardRules.VISIBLE_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgresTestcontainerConfiguration.class)
class JdbcLeaderboardRepositoryIntegrationTest {

    /// An instant every tied entry is forced to share, so only the id can order them.
    private static final Instant SHARED_MOMENT = Instant.parse("2026-01-01T00:00:00Z");

    private static final int TIED_ENTRIES = 4;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void clearLeaderboard() {
        jdbcClient.sql("TRUNCATE TABLE leaderboard_entry RESTART IDENTITY").update();
    }

    @Test
    void flywayCreatesTheLeaderboardTableOnAFreshDatabase() {
        assertThat(leaderboardRepository.findBest(VISIBLE_ENTRIES)).isEmpty();
    }

    @Test
    void storesTheNameGuessCountAndMomentOfTheWin() {
        final var entry = leaderboardRepository.record(new PlayerName("Duke"), 3);

        assertThat(entry.id()).isPositive();
        assertThat(entry.playerName()).isEqualTo("Duke");
        assertThat(entry.guessCount()).isEqualTo(3);
        assertThat(entry.recordedAt()).isNotNull();
    }

    @Test
    void keepsEveryWinRatherThanOverwritingOne() {
        leaderboardRepository.record(new PlayerName("Duke"), 2);
        leaderboardRepository.record(new PlayerName("Duke"), 4);

        assertThat(leaderboardRepository.findBest(VISIBLE_ENTRIES)).hasSize(2);
    }

    @Test
    void ordersByFewestGuesses() {
        leaderboardRepository.record(new PlayerName("Slow"), 5);
        leaderboardRepository.record(new PlayerName("Quick"), 1);
        leaderboardRepository.record(new PlayerName("Middling"), 3);

        assertThat(leaderboardRepository.findBest(VISIBLE_ENTRIES))
                .extracting(LeaderboardEntry::playerName)
                .containsExactly("Quick", "Middling", "Slow");
    }

    @Test
    void breaksTiesByWhichWinWasRecordedFirst() {
        IntStream.rangeClosed(1, 4).forEach(order -> leaderboardRepository.record(new PlayerName("Tied " + order), 2));

        assertThat(leaderboardRepository.findBest(VISIBLE_ENTRIES))
                .extracting(LeaderboardEntry::playerName)
                .containsExactly("Tied 1", "Tied 2", "Tied 3", "Tied 4");
    }

    @Test
    void breaksTiesByInsertionOrderWhenWinsShareATimestamp() {
        final var tied = IntStream.rangeClosed(1, TIED_ENTRIES)
                .mapToObj(order -> leaderboardRepository.record(new PlayerName("Tied " + order), 2))
                .toList();
        // Separate inserts land on distinct clock readings, so recorded_at alone would
        // decide the order. Collapsing the entries onto one instant leaves the id as the
        // only tie-break. Rewriting them newest-first also moves them down the heap, so a
        // query that forgot to order by id would read them back in the wrong order rather
        // than stumbling onto insertion order by luck.
        tied.reversed().forEach(entry -> jdbcClient.sql("""
                        UPDATE leaderboard_entry SET recorded_at = :sharedMoment WHERE id = :id
                        """)
                .param("sharedMoment", Timestamp.from(SHARED_MOMENT))
                .param("id", entry.id())
                .update());

        assertThat(leaderboardRepository.findBest(VISIBLE_ENTRIES))
                .extracting(LeaderboardEntry::playerName)
                .containsExactly("Tied 1", "Tied 2", "Tied 3", "Tied 4");
    }

    @Test
    void readsBackNoMoreThanTheRequestedLimit() {
        IntStream.rangeClosed(1, VISIBLE_ENTRIES + 5)
                .forEach(order -> leaderboardRepository.record(new PlayerName("Player " + order), 2));

        assertThat(leaderboardRepository.findBest(VISIBLE_ENTRIES)).hasSize(VISIBLE_ENTRIES);
    }
}
