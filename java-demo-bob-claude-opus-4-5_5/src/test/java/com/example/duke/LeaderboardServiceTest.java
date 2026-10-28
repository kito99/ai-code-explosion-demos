package com.example.duke.service;

import com.example.duke.model.LeaderboardEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaderboardServiceTest {

    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService = new LeaderboardService();
    }

    @Test
    void getTopEntriesIsEmptyWhenNothingPosted() {
        assertThat(leaderboardService.getTopEntries()).isEmpty();
    }

    @Test
    void recordAndRetrieveSingleEntry() {
        leaderboardService.record("Alice", 3);

        var entries = leaderboardService.getTopEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().playerName()).isEqualTo("Alice");
        assertThat(entries.getFirst().guessCount()).isEqualTo(3);
    }

    @Test
    void entriesOrderedByFewestGuessesFirst() {
        leaderboardService.record("Charlie", 5);
        leaderboardService.record("Alice",   2);
        leaderboardService.record("Bob",     4);

        var entries = leaderboardService.getTopEntries();
        assertThat(entries).extracting(LeaderboardEntry::playerName)
                .containsExactly("Alice", "Bob", "Charlie");
    }

    @Test
    void tiesArebrokenByInsertionOrder() {
        leaderboardService.record("First",  3);
        leaderboardService.record("Second", 3);

        var entries = leaderboardService.getTopEntries();
        assertThat(entries).extracting(LeaderboardEntry::playerName)
                .containsExactly("First", "Second");
    }

    @Test
    void topTenCapIsRespected() {
        IntStream.rangeClosed(1, 15).forEach(i -> leaderboardService.record("Player" + i, i));

        assertThat(leaderboardService.getTopEntries()).hasSize(10);
    }

    @Test
    void topTenContainsLowestGuessCountEntries() {
        IntStream.rangeClosed(1, 15).forEach(i -> leaderboardService.record("Player" + i, i));

        var entries = leaderboardService.getTopEntries();
        assertThat(entries).extracting(LeaderboardEntry::guessCount)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void blankNameThrows() {
        assertThatThrownBy(() -> leaderboardService.record("   ", 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyNameThrows() {
        assertThatThrownBy(() -> leaderboardService.record("", 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nameTooLongThrows() {
        var longName = "A".repeat(31);
        assertThatThrownBy(() -> leaderboardService.record(longName, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30");
    }

    @Test
    void maxLengthNameIsAccepted() {
        var exactName = "A".repeat(30);
        var entry = leaderboardService.record(exactName, 3);
        assertThat(entry.playerName()).isEqualTo(exactName);
    }

    @Test
    void nameSurroundingWhitespaceIsTrimmed() {
        leaderboardService.record("  Alice  ", 3);
        assertThat(leaderboardService.getTopEntries().getFirst().playerName()).isEqualTo("Alice");
    }

    @Test
    void eachWinCreatesItsOwnEntry() {
        leaderboardService.record("Alice", 2);
        leaderboardService.record("Alice", 2);

        assertThat(leaderboardService.getTopEntries()).hasSize(2);
    }

    @Test
    void clearRemovesAllEntries() {
        leaderboardService.record("Alice", 2);
        leaderboardService.clear();

        assertThat(leaderboardService.getTopEntries()).isEmpty();
    }
}
