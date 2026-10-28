package com.example.guessgame.leaderboard;

import com.example.guessgame.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for leaderboard persistence. Uses Testcontainers (via AbstractIntegrationTest).
 */
class LeaderboardServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Test
    void recordWin_persists_andAppearsInTopEntries() {
        leaderboardRepository.deleteAll();
        leaderboardService.recordWin("Alice", 3);

        var entries = leaderboardService.getTopEntries();
        assertThat(entries).anyMatch(e ->
                e.getPlayerName().equals("Alice") && e.getGuessCount() == 3);
    }

    @Test
    void topEntries_orderedByFewestGuessesThenOldest() {
        leaderboardRepository.deleteAll();

        leaderboardService.recordWin("Bob", 4);
        leaderboardService.recordWin("Carol", 2);
        leaderboardService.recordWin("Dave", 2);

        var entries = leaderboardService.getTopEntries();
        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).getPlayerName()).isEqualTo("Carol"); // 2 guesses, earliest
        assertThat(entries.get(1).getPlayerName()).isEqualTo("Dave");  // 2 guesses, later
        assertThat(entries.get(2).getPlayerName()).isEqualTo("Bob");   // 4 guesses
    }

    @Test
    void topEntries_cappedAtTen() {
        leaderboardRepository.deleteAll();
        for (int i = 0; i < 12; i++) {
            leaderboardService.recordWin("Player" + i, 1);
        }

        assertThat(leaderboardService.getTopEntries()).hasSize(10);
    }

    @Test
    void emptyLeaderboard_returnsEmptyList() {
        leaderboardRepository.deleteAll();
        assertThat(leaderboardService.getTopEntries()).isEmpty();
    }
}
