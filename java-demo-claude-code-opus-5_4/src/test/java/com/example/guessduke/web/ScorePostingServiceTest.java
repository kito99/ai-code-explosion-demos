package com.example.guessduke.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.dao.DataAccessResourceFailureException;

import com.example.guessduke.game.GuessingGame;
import com.example.guessduke.leaderboard.InMemoryLeaderboardRepository;
import com.example.guessduke.leaderboard.LeaderboardEntry;
import com.example.guessduke.leaderboard.LeaderboardRepository;
import com.example.guessduke.leaderboard.PlayerName;

import static com.example.guessduke.game.GameRules.MAX_GUESSES;
import static com.example.guessduke.leaderboard.LeaderboardRules.MAX_NAME_LENGTH;
import static com.example.guessduke.leaderboard.LeaderboardRules.VISIBLE_ENTRIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ScorePostingServiceTest {

    private static final int SECRET = 4;

    private final InMemoryLeaderboardRepository repository = new InMemoryLeaderboardRepository();
    private final ScorePostingService scorePostingService = new ScorePostingService(repository);

    @Test
    void recordsAWinUnderTheTrimmedName() {
        final var game = wonGame(2);

        final var submission = scorePostingService.postScore(game, "  Duke  ");

        assertThat(submission.posted()).isTrue();
        assertThat(submission.message()).contains("Duke");
        assertThat(repository.findBest(VISIBLE_ENTRIES))
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.playerName()).isEqualTo("Duke");
                    assertThat(entry.guessCount()).isEqualTo(2);
                });
    }

    @ParameterizedTest(name = "a name of \"{0}\" records nothing")
    @ValueSource(strings = {"", "    ", "\t"})
    @NullSource
    void recordsNothingForAnUnusableName(final String rawName) {
        final var submission = scorePostingService.postScore(wonGame(1), rawName);

        assertThat(submission.posted()).isFalse();
        assertThat(submission.message()).isEqualTo(GameMessages.NAME_NOT_ACCEPTED);
        assertThat(repository.findBest(VISIBLE_ENTRIES)).isEmpty();
    }

    @Test
    void recordsNothingForANameThatIsTooLong() {
        final var submission = scorePostingService.postScore(wonGame(1), "D".repeat(MAX_NAME_LENGTH + 1));

        assertThat(submission.posted()).isFalse();
        assertThat(repository.findBest(VISIBLE_ENTRIES)).isEmpty();
    }

    @Test
    void recordsNothingForALostRound() {
        final var game = new GuessingGame(SECRET);
        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            game.submitGuess(SECRET + 1);
        }

        final var submission = scorePostingService.postScore(game, "Duke");

        assertThat(submission.posted()).isFalse();
        assertThat(submission.message()).isEqualTo(GameMessages.NO_SCORE_TO_POST);
        assertThat(repository.findBest(VISIBLE_ENTRIES)).isEmpty();
    }

    @Test
    void recordsNothingForARoundStillInPlay() {
        final var submission = scorePostingService.postScore(new GuessingGame(SECRET), "Duke");

        assertThat(submission.posted()).isFalse();
        assertThat(repository.findBest(VISIBLE_ENTRIES)).isEmpty();
    }

    @Test
    void recordsOneEntryWhenTheSameWinIsPostedTwice() {
        final var game = wonGame(1);
        scorePostingService.postScore(game, "Duke");

        final var second = scorePostingService.postScore(game, "Duke");

        assertThat(second.posted()).isFalse();
        assertThat(second.message()).isEqualTo(GameMessages.SCORE_ALREADY_POSTED);
        assertThat(repository.findBest(VISIBLE_ENTRIES)).hasSize(1);
    }

    @Test
    void leavesTheWinPostableWhenStoringFails() {
        final var failing = new ScorePostingService(new LeaderboardRepository() {
            @Override
            public LeaderboardEntry record(final PlayerName playerName, final int guessCount) {
                throw new DataAccessResourceFailureException("database is down");
            }

            @Override
            public List<LeaderboardEntry> findBest(final int limit) {
                return List.of();
            }
        });
        final var game = wonGame(1);

        assertThatExceptionOfType(DataAccessResourceFailureException.class)
                .isThrownBy(() -> failing.postScore(game, "Duke"));

        assertThat(game.isScorePosted()).isFalse();
    }

    private static GuessingGame wonGame(final int guessesTaken) {
        final var game = new GuessingGame(SECRET);
        for (var attempt = 1; attempt < guessesTaken; attempt++) {
            game.submitGuess(SECRET + 1);
        }
        game.submitGuess(SECRET);
        return game;
    }
}
