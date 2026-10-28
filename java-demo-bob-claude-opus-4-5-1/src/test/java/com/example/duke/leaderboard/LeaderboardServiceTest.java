package com.example.duke.leaderboard;

import com.example.duke.game.GameService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Random;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LeaderboardServiceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private LeaderboardRepository repository;

    private LeaderboardService leaderboardService;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        // Fixed random: nextInt(10) always returns 4, so secret = 5.
        gameService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        leaderboardService = new LeaderboardService(repository, gameService);
    }

    @Test
    void listTop10_emptyWhenNothingRecorded() {
        assertThat(leaderboardService.listTop10()).isEmpty();
    }

    @Test
    void postScore_recordsWonGame() {
        var game = gameService.startGame(); // secret = 5
        gameService.guess(game.gameId(), 5); // win in 1 guess

        var entry = leaderboardService.postScore(game.gameId(), "Alice");

        assertThat(entry.playerName()).isEqualTo("Alice");
        assertThat(entry.guessCount()).isEqualTo(1);
        assertThat(leaderboardService.listTop10()).hasSize(1);
    }

    @Test
    void postScore_rejectsBlankName() {
        var game = gameService.startGame();
        gameService.guess(game.gameId(), 5);

        assertThatThrownBy(() -> leaderboardService.postScore(game.gameId(), "   "))
                .isInstanceOf(InvalidPlayerNameException.class);
    }

    @Test
    void postScore_rejectsNameLongerThan30Chars() {
        var game = gameService.startGame();
        gameService.guess(game.gameId(), 5);

        assertThatThrownBy(() -> leaderboardService.postScore(game.gameId(), "A".repeat(31)))
                .isInstanceOf(InvalidPlayerNameException.class);
    }

    @Test
    void postScore_accepts30CharName() {
        var game = gameService.startGame();
        gameService.guess(game.gameId(), 5);

        var entry = leaderboardService.postScore(game.gameId(), "A".repeat(30));

        assertThat(entry.playerName()).isEqualTo("A".repeat(30));
    }

    @Test
    void postScore_rejectsNonWonGame() {
        var game = gameService.startGame();
        // game is still ACTIVE — not won

        assertThatThrownBy(() -> leaderboardService.postScore(game.gameId(), "Bob"))
                .isInstanceOf(InvalidScoreException.class);
    }

    @Test
    void postScore_rejectsDuplicatePostForSameGame() {
        var game = gameService.startGame();
        gameService.guess(game.gameId(), 5);
        leaderboardService.postScore(game.gameId(), "Alice");

        // Second post for the same gameId must be rejected.
        assertThatThrownBy(() -> leaderboardService.postScore(game.gameId(), "Alice Again"))
                .isInstanceOf(InvalidScoreException.class)
                .hasMessageContaining("already been posted");
    }

    @Test
    void listTop10_orderedByFewestGuessesAndThenOldest() {
        // Use two independent services so each game is tracked in its own session map.
        var svcA = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var svcB = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5

        var gameA = svcA.startGame();
        svcA.guess(gameA.gameId(), 5); // 1 guess

        var gameB = svcB.startGame();
        svcB.guess(gameB.gameId(), 3); // wrong
        svcB.guess(gameB.gameId(), 5); // correct — 2 guesses

        // Post Bob first (2 guesses), then Alice (1 guess) — ordering must be by guessCount.
        new LeaderboardService(repository, svcB).postScore(gameB.gameId(), "Bob");
        new LeaderboardService(repository, svcA).postScore(gameA.gameId(), "Alice");

        var top = new LeaderboardService(repository, svcA).listTop10();

        assertThat(top).hasSize(2);
        // Alice (1 guess) must outrank Bob (2 guesses) regardless of insertion order.
        assertThat(top.get(0).playerName()).isEqualTo("Alice");
        assertThat(top.get(0).guessCount()).isEqualTo(1);
        assertThat(top.get(1).playerName()).isEqualTo("Bob");
        assertThat(top.get(0).rank()).isEqualTo(1);
        assertThat(top.get(1).rank()).isEqualTo(2);
    }

    @Test
    void listTop10_tiesBrokenByOldestFirst() {
        // Two separate service instances both produce secret=5 (same FixedRandom).
        var svcA = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var svcB = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var lbSvc = new LeaderboardService(repository, svcA);

        var gA = svcA.startGame();
        svcA.guess(gA.gameId(), 5); // 1 guess

        // Small sleep to ensure recordedAt differs
        var gB = svcB.startGame();
        svcB.guess(gB.gameId(), 5); // 1 guess

        // Manually register svcB result by swapping service reference temporarily
        lbSvc = new LeaderboardService(repository, svcA);
        lbSvc.postScore(gA.gameId(), "First");  // recorded earlier

        lbSvc = new LeaderboardService(repository, svcB);
        lbSvc.postScore(gB.gameId(), "Second"); // recorded later

        // Both have 1 guess — "First" should rank above "Second" (inserted earlier).
        var lbRead = new LeaderboardService(repository, svcA);
        var top = lbRead.listTop10();

        assertThat(top).hasSize(2);
        assertThat(top.get(0).playerName()).isEqualTo("First");
        assertThat(top.get(1).playerName()).isEqualTo("Second");
    }

    @Test
    void listTop10_returnsAtMost10Entries() {
        for (int i = 0; i < 15; i++) {
            var g = gameService.startGame();
            gameService.guess(g.gameId(), 5);
            leaderboardService.postScore(g.gameId(), "Player" + i);
        }
        assertThat(leaderboardService.listTop10()).hasSize(10);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static final class FixedRandom extends Random {
        private final int fixedValue;

        FixedRandom(int fixedValue) {
            super();
            this.fixedValue = fixedValue;
        }

        @Override
        public int nextInt(int bound) {
            return fixedValue % bound;
        }
    }
}
