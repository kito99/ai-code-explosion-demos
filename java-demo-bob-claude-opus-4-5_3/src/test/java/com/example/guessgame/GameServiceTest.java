package com.example.guessgame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-level tests for GameService with a real database (Testcontainers).
 */
@SpringBootTest
@ActiveProfiles("test")
class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @BeforeEach
    void clearLeaderboard() {
        leaderboardRepository.deleteAll();
    }

    @Test
    void startGameCreatesANewSession() {
        var sessionId = gameService.startGame();
        assertThat(sessionId).isNotBlank();
        assertThat(gameService.getGame(sessionId)).isNotNull();
    }

    @Test
    void getGameThrowsForUnknownSession() {
        assertThatThrownBy(() -> gameService.getGame("no-such-session"))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void submitGuessReturnsNonNullResult() {
        var sessionId = gameService.startGame();
        var result = gameService.submitGuess(sessionId, 5);
        assertThat(result).isNotNull();
    }

    @Test
    void recordScoreThrowsWhenGameNotWon() {
        var sessionId = gameService.startGame();
        assertThatThrownBy(() -> gameService.recordScore(sessionId, "Alice"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordScorePersistsEntry() {
        var sessionId = winGame();
        gameService.recordScore(sessionId, "TestPlayer");

        var entries = gameService.getLeaderboard();
        assertThat(entries).anyMatch(e -> e.getPlayerName().equals("TestPlayer"));
    }

    @Test
    void recordScoreThrowsOnDuplicatePost() {
        var sessionId = winGame();
        gameService.recordScore(sessionId, "FirstPost");
        assertThatThrownBy(() -> gameService.recordScore(sessionId, "SecondPost"))
                .isInstanceOf(ScoreAlreadyPostedException.class);
    }

    @Test
    void leaderboardIsOrderedByGuessCountThenTime() {
        var s1 = winGame();
        gameService.recordScore(s1, "SomePlayer");

        var s2 = winGameInOneGuess();
        gameService.recordScore(s2, "OneGuesser");

        var board = gameService.getLeaderboard();
        assertThat(board).isNotEmpty();
        for (int i = 1; i < board.size(); i++) {
            assertThat(board.get(i).getGuessCount())
                    .isGreaterThanOrEqualTo(board.get(i - 1).getGuessCount());
        }
    }

    @Test
    void leaderboardCapsAtTen() {
        for (int i = 0; i < 12; i++) {
            var s = winGame();
            gameService.recordScore(s, "Player" + i);
        }
        assertThat(gameService.getLeaderboard()).hasSizeLessThanOrEqualTo(10);
    }

    // ---- helpers ----

    /**
     * Wins a game by brute-forcing numbers 1–10. Restarts if the game ends without a win
     * (possible when the secret is not found within 5 guesses in a single pass).
     */
    private String winGame() {
        for (int attempt = 0; attempt < 20; attempt++) {
            var sessionId = gameService.startGame();
            for (int n = 1; n <= 10; n++) {
                var result = gameService.submitGuess(sessionId, n);
                if (result == GuessResult.CORRECT) {
                    return sessionId;
                }
                if (result == GuessResult.GAME_OVER) {
                    break; // start a fresh game
                }
            }
        }
        throw new IllegalStateException("Could not win after 20 attempts.");
    }

    /** Forces a win on guess 1 using reflection. */
    private String winGameInOneGuess() {
        var sessionId = gameService.startGame();
        try {
            var game = gameService.getGame(sessionId);
            var field = Game.class.getDeclaredField("secretNumber");
            field.setAccessible(true);
            var secret = (int) field.get(game);
            gameService.submitGuess(sessionId, secret);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sessionId;
    }
}
