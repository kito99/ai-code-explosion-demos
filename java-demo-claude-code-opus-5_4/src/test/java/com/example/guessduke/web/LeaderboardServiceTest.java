package com.example.guessduke.web;

import java.util.stream.IntStream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import com.example.guessduke.leaderboard.InMemoryLeaderboardRepository;
import com.example.guessduke.leaderboard.PlayerName;

import static com.example.guessduke.leaderboard.LeaderboardRules.VISIBLE_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;

class LeaderboardServiceTest {

    private final InMemoryLeaderboardRepository repository = new InMemoryLeaderboardRepository();
    private final LeaderboardService leaderboardService = new LeaderboardService(repository);

    @Test
    void reportsNothingBeforeAnyWinIsPosted() {
        assertThat(leaderboardService.bestRounds()).isEmpty();
    }

    @Test
    void ranksFewestGuessesFirst() {
        post("Slow", 5);
        post("Quick", 1);
        post("Middling", 3);

        assertThat(leaderboardService.bestRounds())
                .extracting(LeaderboardRowView::rank, LeaderboardRowView::playerName, LeaderboardRowView::guessCount)
                .containsExactly(
                        Tuple.tuple(1, "Quick", 1),
                        Tuple.tuple(2, "Middling", 3),
                        Tuple.tuple(3, "Slow", 5));
    }

    @Test
    void showsNoMoreRowsThanTheScreenHolds() {
        IntStream.rangeClosed(1, VISIBLE_ENTRIES + 4).forEach(entry -> post("Player " + entry, 2));

        assertThat(leaderboardService.bestRounds()).hasSize(VISIBLE_ENTRIES);
    }

    private void post(final String playerName, final int guessCount) {
        repository.record(new PlayerName(playerName), guessCount);
    }
}
