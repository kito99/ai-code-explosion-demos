package com.example.duke.game;

import com.example.duke.config.GameProperties;
import com.example.duke.leaderboard.InvalidPlayerNameException;
import com.example.duke.leaderboard.LeaderboardService;
import com.example.duke.leaderboard.PlayerName;
import com.example.duke.leaderboard.PostedScore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.dao.DataAccessResourceFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.example.duke.game.GameStatus.LOST;
import static com.example.duke.game.GameStatus.WON;
import static com.example.duke.game.GuessOutcome.TOO_HIGH;
import static com.example.duke.game.GuessOutcome.TOO_LOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class GameServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final int SECRET = 7;

    private final LeaderboardService leaderboardService = mock(LeaderboardService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private GameService gameService;

    @BeforeEach
    void setUp() {
        final var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        final var properties = new GameProperties(Duration.ofMinutes(30), Duration.ofMinutes(5));
        final var store = new GameStore(properties, clock);
        gameService = new GameService(store, () -> SECRET, leaderboardService,
                new GameMetrics(meterRegistry, store), clock);
    }

    @Test
    @DisplayName("a started game is ready to play and is counted")
    void startsGame() {
        final var game = gameService.startGame();

        assertThat(game.secretNumber()).isEqualTo(SECRET);
        assertThat(game.guessesRemaining()).isEqualTo(GameRules.MAX_GUESSES);
        assertThat(meterRegistry.counter("duke.games.started").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("feedback says whether the guess was too high or too low")
    void reportsHighAndLow() {
        final var gameId = gameService.startGame().id();

        assertThat(gameService.submitGuess(gameId, 2).outcome()).isEqualTo(TOO_LOW);
        assertThat(gameService.submitGuess(gameId, 9).outcome()).isEqualTo(TOO_HIGH);
    }

    @Test
    @DisplayName("an out-of-range guess is refused and does not count")
    void refusesOutOfRangeGuess() {
        final var gameId = gameService.startGame().id();
        gameService.submitGuess(gameId, 2);

        assertThatThrownBy(() -> gameService.submitGuess(gameId, 11))
                .isInstanceOf(InvalidGuessException.class);

        final var afterward = gameService.submitGuess(gameId, 3).game();
        assertThat(afterward.guessesMade()).isEqualTo(2);
        assertThat(meterRegistry.counter("duke.guesses.rejected").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("winning records the guess count it took")
    void winsAndMeasuresGuesses() {
        final var gameId = gameService.startGame().id();
        gameService.submitGuess(gameId, 2);
        final var result = gameService.submitGuess(gameId, SECRET);

        assertThat(result.game().status()).isEqualTo(WON);
        assertThat(result.game().guessesMade()).isEqualTo(2);
        assertThat(meterRegistry.summary("duke.wins.guesses").takeSnapshot().total()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("running out of guesses loses the game")
    void losesAfterTheLastGuess() {
        final var gameId = gameService.startGame().id();
        for (var attempt = 0; attempt < GameRules.MAX_GUESSES - 1; attempt++) {
            gameService.submitGuess(gameId, 1);
        }
        final var result = gameService.submitGuess(gameId, 1);

        assertThat(result.game().status()).isEqualTo(LOST);
        assertThat(result.game().revealedSecretNumber()).contains(SECRET);
        assertThat(meterRegistry.counter("duke.games.finished", "outcome", "lost").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("an unknown game id is reported as missing")
    void rejectsUnknownGame() {
        assertThatThrownBy(() -> gameService.submitGuess(UUID.randomUUID(), 5))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("a win is posted once, with the guess count the server holds")
    void postsWinningScore() {
        when(leaderboardService.record(any(), anyInt()))
                .thenReturn(new PostedScore("Duke Fan", 1, NOW));
        final var gameId = gameService.startGame().id();
        gameService.submitGuess(gameId, SECRET);

        final var posted = gameService.postScore(gameId, "  Duke Fan  ");

        assertThat(posted.name()).isEqualTo("Duke Fan");
        verify(leaderboardService).record(new PlayerName("Duke Fan"), 1);
        assertThatThrownBy(() -> gameService.postScore(gameId, "Duke Fan"))
                .isInstanceOf(ScoreAlreadyPostedException.class);
    }

    @Test
    @DisplayName("a win that cannot be stored can be posted again once the database is back")
    void releasesTheWinWhenRecordingFails() {
        final var gameId = gameService.startGame().id();
        gameService.submitGuess(gameId, SECRET);
        when(leaderboardService.record(any(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("database is down"))
                .thenReturn(new PostedScore("Duke Fan", 1, NOW));

        assertThatThrownBy(() -> gameService.postScore(gameId, "Duke Fan"))
                .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(gameService.postScore(gameId, "Duke Fan").name()).isEqualTo("Duke Fan");
        verify(leaderboardService, times(2)).record(new PlayerName("Duke Fan"), 1);
    }

    @Test
    @DisplayName("a lost game records nothing")
    void refusesToPostALoss() {
        final var gameId = gameService.startGame().id();
        List.of(1, 2, 3, 4, 5).forEach(guess -> gameService.submitGuess(gameId, guess == SECRET ? 1 : guess));

        assertThatThrownBy(() -> gameService.postScore(gameId, "Duke Fan"))
                .isInstanceOf(GameNotWonException.class);
        verify(leaderboardService, never()).record(any(), anyInt());
    }

    @Test
    @DisplayName("an unusable name records nothing")
    void refusesToPostABlankName() {
        final var gameId = gameService.startGame().id();
        gameService.submitGuess(gameId, SECRET);

        assertThatThrownBy(() -> gameService.postScore(gameId, "   "))
                .isInstanceOf(InvalidPlayerNameException.class);
        verify(leaderboardService, never()).record(any(), anyInt());
    }
}
