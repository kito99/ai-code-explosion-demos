package com.example.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcLeaderboardRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcLeaderboardRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = PostgresTestSupport.newDataSource();
        SchemaInitializer.initialize(dataSource);
        PostgresTestSupport.clearLeaderboard(dataSource);
        repository = new JdbcLeaderboardRepository(dataSource);
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void emptyLeaderboardHasNoEntries() {
        assertTrue(repository.findTop(GameRules.LEADERBOARD_SIZE).isEmpty());
    }

    @Test
    void ordersByFewestGuessesThenEarliestRecorded() {
        repository.save("slow", 4);
        repository.save("first-tie", 2);
        repository.save("fastest", 1);
        repository.save("second-tie", 2);

        final var names = repository.findTop(GameRules.LEADERBOARD_SIZE).stream()
                .map(LeaderboardEntry::playerName).toList();
        assertEquals(List.of("fastest", "first-tie", "second-tie", "slow"), names);
    }

    @Test
    void duplicateNamesCreateSeparateEntries() {
        repository.save("Duke", 3);
        repository.save("Duke", 3);
        assertEquals(2, repository.findTop(GameRules.LEADERBOARD_SIZE).size());
    }

    @Test
    void returnsAtMostTenEntries() {
        for (var index = 0; index < GameRules.LEADERBOARD_SIZE + 3; index++) {
            repository.save("player-" + index, 3);
        }
        assertEquals(GameRules.LEADERBOARD_SIZE, repository.findTop(GameRules.LEADERBOARD_SIZE).size());
    }

    @Test
    void schemaInitializationIsRepeatableAndEntriesSurviveIt() {
        repository.save("Duke", 2);
        SchemaInitializer.initialize(dataSource);
        assertEquals(1, repository.findTop(GameRules.LEADERBOARD_SIZE).size());
    }
}
