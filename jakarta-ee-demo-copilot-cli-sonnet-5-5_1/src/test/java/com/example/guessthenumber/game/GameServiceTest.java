package com.example.guessthenumber.game;

import com.example.guessthenumber.telemetry.GameMetrics;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameServiceTest {

    private static final int SECRET = 3;

    private CurrentGame currentGame;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        currentGame = new CurrentGame();
        gameService = new GameService(currentGame, rules -> SECRET,
                new GameMetrics(OpenTelemetry.noop().getMeter(GameServiceTest.class.getName())));
    }

    @Test
    void startingAGamePutsItInTheSession() {
        final var game = gameService.startNewGame();

        assertSame(game, currentGame.require());
        assertEquals(GameStatus.ACTIVE, game.status());
    }

    @Test
    void startingAGameReplacesTheOneInProgress() {
        final var first = gameService.startNewGame();
        first.guess(SECRET + 1);

        final var second = gameService.startNewGame();

        assertEquals(0, second.guessesMade());
        assertSame(second, currentGame.require());
    }

    @Test
    void reportsNoGameBeforeOneIsStarted() {
        assertThrows(NoGameInProgressException.class, () -> gameService.submitGuess(SECRET));
        assertThrows(NoGameInProgressException.class, gameService::requireGame);
    }

    @Test
    void returnsTheOutcomeAndTheGameForAGuess() {
        gameService.startNewGame();

        final var result = gameService.submitGuess(SECRET + 1);

        assertEquals(GuessOutcome.TOO_HIGH, result.outcome());
        assertEquals(1, result.game().guessesMade());
    }

    @Test
    void treatsAMissingGuessAsInvalid() {
        gameService.startNewGame();

        assertThrows(InvalidGuessException.class, () -> gameService.submitGuess(null));
        assertEquals(0, currentGame.require().guessesMade());
    }

    @Test
    void refusesToHandOutAWinThatWasNotPlayed() {
        gameService.startNewGame();

        assertThrows(NoWinningGameException.class, gameService::requireWonGame);
        assertThrows(NoWinningGameException.class, gameService::requireUnpostedWonGame);
    }

    @Test
    void refusesToHandOutAWinThatIsAlreadyPosted() {
        gameService.startNewGame();
        gameService.submitGuess(SECRET);
        gameService.requireUnpostedWonGame().markScorePosted();

        assertThrows(ScoreAlreadyPostedException.class, gameService::requireUnpostedWonGame);
    }
}
