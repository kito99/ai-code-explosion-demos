package com.example.duke.leaderboard;

import com.example.duke.support.PostgresContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(PostgresContainerConfiguration.class)
class LeaderboardServiceIntegrationTest {

    @Autowired
    private LeaderboardService service;

    @Autowired
    private LeaderboardRepository repository;

    @BeforeEach
    void clearLeaderboard() {
        repository.deleteAll();
    }

    @Test
    void startsEmpty() {
        assertThat(service.topStandings()).isEmpty();
    }

    @Test
    void ranksByFewestGuessesThenEarliest() {
        service.record(new PlayerName("Three"), 3);
        service.record(new PlayerName("FirstTwo"), 2);
        service.record(new PlayerName("SecondTwo"), 2);

        assertThat(service.topStandings())
                .extracting(LeaderboardStanding::rank, LeaderboardStanding::name, LeaderboardStanding::guesses)
                .containsExactly(tuple(1, "FirstTwo", 2), tuple(2, "SecondTwo", 2), tuple(3, "Three", 3));
    }

    @Test
    void sameNameMayAppearTwiceAndOnlyTopTenAreShown() {
        IntStream.range(0, 12).forEach(_ -> service.record(new PlayerName("Duke"), 4));
        service.record(new PlayerName("Best"), 1);

        assertThat(service.topStandings()).hasSize(LeaderboardService.TOP_ENTRIES);
        assertThat(service.topStandings().getFirst().name()).isEqualTo("Best");
    }
}
