package com.example.duke.leaderboard;

import com.example.duke.support.PostgresContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.stream.IntStream;

import static com.example.duke.leaderboard.LeaderboardService.TOP_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(PostgresContainerConfiguration.class)
class LeaderboardServiceIntegrationTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private LeaderboardRepository repository;

    @BeforeEach
    void clearLeaderboard() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("an empty leaderboard has no standings")
    void startsEmpty() {
        assertThat(leaderboardService.topStandings()).isEmpty();
    }

    @Test
    @DisplayName("fewer guesses rank higher")
    void ranksFewestGuessesFirst() {
        leaderboardService.record(new PlayerName("Three"), 3);
        leaderboardService.record(new PlayerName("One"), 1);
        leaderboardService.record(new PlayerName("Two"), 2);

        assertThat(leaderboardService.topStandings())
                .extracting(LeaderboardStanding::rank, LeaderboardStanding::name, LeaderboardStanding::guesses)
                .containsExactly(tuple(1, "One", 1), tuple(2, "Two", 2), tuple(3, "Three", 3));
    }

    @Test
    @DisplayName("a tie goes to whoever was recorded first")
    void breaksTiesByRecordingOrder() {
        leaderboardService.record(new PlayerName("First"), 2);
        leaderboardService.record(new PlayerName("Second"), 2);

        assertThat(leaderboardService.topStandings())
                .extracting(LeaderboardStanding::name)
                .containsExactly("First", "Second");
    }

    @Test
    @DisplayName("every win gets its own entry, and only the best ten are shown")
    void showsAtMostTenEntries() {
        IntStream.rangeClosed(1, TOP_ENTRIES + 2)
                .forEach(index -> leaderboardService.record(new PlayerName("Player " + index), 4));

        assertThat(repository.count()).isEqualTo(TOP_ENTRIES + 2);
        assertThat(leaderboardService.topStandings()).hasSize(TOP_ENTRIES);
    }
}
