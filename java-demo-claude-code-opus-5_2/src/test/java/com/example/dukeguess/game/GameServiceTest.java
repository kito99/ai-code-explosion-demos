package com.example.dukeguess.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessResourceFailureException;

import com.example.dukeguess.config.GameProperties;
import com.example.dukeguess.leaderboard.InvalidPlayerNameException;
import com.example.dukeguess.leaderboard.LeaderboardEntry;
import com.example.dukeguess.leaderboard.LeaderboardService;
import com.example.dukeguess.leaderboard.PlayerName;
import com.example.dukeguess.telemetry.GameMetrics;
import com.example.dukeguess.telemetry.GameTelemetry;

import static com.example.dukeguess.game.GameRules.MAX_GUESSES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GameServiceTest {

    private static final int SECRET = 7;
    private static final int WRONG = SECRET - 1;
    private static final Instant RECORDED_AT = Instant.parse("2026-01-01T12:00:00Z");

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final LeaderboardService leaderboardService = mock(LeaderboardService.class);
    private final GameSessions gameSessions = new GameSessions(
            () -> SECRET,
            new GameProperties(Duration.ofHours(2)),
            Clock.fixed(RECORDED_AT, ZoneOffset.UTC));

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(
                gameSessions,
                leaderboardService,
                new GameMetrics(meterRegistry, gameSessions),
                new GameTelemetry(ObservationRegistry.NOOP));
    }

    private double counter(final String name, final String... tags) {
        final var counter = meterRegistry.find(name).tags(tags).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private UUID startedGameId() {
        return gameService.startGame().gameId();
    }

    private UUID wonGameId() {
        final var gameId = startedGameId();
        gameService.submitGuess(gameId, SECRET);
        return gameId;
    }

    @Test
    void startsAGameReadyToPlayAndCountsIt() {
        final var snapshot = gameService.startGame();

        assertThat(snapshot.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(snapshot.guessesRemaining()).isEqualTo(MAX_GUESSES);
        assertThat(counter("duke.games.started")).isEqualTo(1.0);
    }

    @Test
    void countsAnAcceptedGuessUnderItsVerdict() {
        final var gameId = startedGameId();

        final var outcome = gameService.submitGuess(gameId, WRONG);

        assertThat(outcome.verdict()).isEqualTo(GuessVerdict.TOO_LOW);
        assertThat(counter("duke.guesses.accepted", "verdict", "too_low")).isEqualTo(1.0);
    }

    @Test
    void countsARejectedGuessSeparatelyAndSpendsNone() {
        final var gameId = startedGameId();

        assertThatExceptionOfType(InvalidGuessException.class)
                .isThrownBy(() -> gameService.submitGuess(gameId, 42));

        assertThat(counter("duke.guesses.rejected")).isEqualTo(1.0);
        assertThat(counter("duke.guesses.accepted", "verdict", "correct")).isZero();
        assertThat(gameSessions.require(gameId).guessesMade()).isZero();
    }

    @Test
    void countsAWinAndHowManyGuessesItTook() {
        final var gameId = startedGameId();
        gameService.submitGuess(gameId, WRONG);

        gameService.submitGuess(gameId, SECRET);

        assertThat(counter("duke.games.finished", "outcome", "won")).isEqualTo(1.0);
        assertThat(meterRegistry.find("duke.wins.guesses").summary().totalAmount()).isEqualTo(2.0);
    }

    @Test
    void countsALossOnceTheGuessesRunOut() {
        final var gameId = startedGameId();

        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            gameService.submitGuess(gameId, WRONG);
        }

        assertThat(counter("duke.games.finished", "outcome", "lost")).isEqualTo(1.0);
        assertThat(meterRegistry.find("duke.wins.guesses").summary().count()).isZero();
    }

    @Test
    void rejectsAGuessForAGameTheServerNoLongerHolds() {
        final var unknownId = UUID.randomUUID();

        assertThatExceptionOfType(GameNotFoundException.class)
                .isThrownBy(() -> gameService.submitGuess(unknownId, SECRET));
    }

    @Test
    void postsAWinUnderTheGuessCountTheServerScored() {
        final var gameId = startedGameId();
        gameService.submitGuess(gameId, WRONG);
        gameService.submitGuess(gameId, SECRET);
        given(leaderboardService.record(any(PlayerName.class), anyInt()))
                .willReturn(new LeaderboardEntry(new PlayerName("Duke"), 2, RECORDED_AT));

        final var entry = gameService.postScore(gameId, "  Duke  ");

        assertThat(entry.guesses()).isEqualTo(2);
        assertThat(counter("duke.scores.posted")).isEqualTo(1.0);
    }

    @Test
    void refusesToPostAGameThatIsStillInPlay() {
        final var gameId = startedGameId();

        assertThatExceptionOfType(GameNotWonException.class)
                .isThrownBy(() -> gameService.postScore(gameId, "Duke"));
    }

    @Test
    void refusesToPostALostGame() {
        final var gameId = startedGameId();
        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            gameService.submitGuess(gameId, WRONG);
        }

        assertThatExceptionOfType(GameNotWonException.class)
                .isThrownBy(() -> gameService.postScore(gameId, "Duke"));
    }

    @Test
    void refusesToPostTheSameWinTwice() {
        final var gameId = wonGameId();
        given(leaderboardService.record(any(PlayerName.class), anyInt()))
                .willReturn(new LeaderboardEntry(new PlayerName("Duke"), 1, RECORDED_AT));
        gameService.postScore(gameId, "Duke");

        assertThatExceptionOfType(ScoreAlreadyPostedException.class)
                .isThrownBy(() -> gameService.postScore(gameId, "Duke again"));
    }

    @Test
    void turnsAwayAnUnusableNameWithoutBurningTheWin() {
        final var gameId = wonGameId();
        given(leaderboardService.record(any(PlayerName.class), anyInt()))
                .willReturn(new LeaderboardEntry(new PlayerName("Duke"), 1, RECORDED_AT));

        assertThatExceptionOfType(InvalidPlayerNameException.class)
                .isThrownBy(() -> gameService.postScore(gameId, "   "));

        assertThat(gameService.postScore(gameId, "Duke").guesses()).isEqualTo(1);
    }

    @Test
    void leavesTheWinPostableWhenTheDatabaseIsDown() {
        final var gameId = wonGameId();
        given(leaderboardService.record(any(PlayerName.class), anyInt()))
                .willThrow(new DataAccessResourceFailureException("database is down"))
                .willReturn(new LeaderboardEntry(new PlayerName("Duke"), 1, RECORDED_AT));

        assertThatExceptionOfType(DataAccessResourceFailureException.class)
                .isThrownBy(() -> gameService.postScore(gameId, "Duke"));

        assertThat(gameService.postScore(gameId, "Duke").guesses()).isEqualTo(1);
        assertThat(counter("duke.scores.posted")).isEqualTo(1.0);
    }
}
