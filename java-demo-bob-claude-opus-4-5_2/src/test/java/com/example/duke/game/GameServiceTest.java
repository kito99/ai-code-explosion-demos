package com.example.duke.game;

import com.example.duke.FixedRandom;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GameServiceTest {

    private GameService service;

    @BeforeEach
    void setUp() {
        service = new GameService(new SimpleMeterRegistry());
    }

    @Test
    void startGame_returnsActiveGameWithFiveGuessesRemaining() {
        var game = service.startGame();

        assertThat(game.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(game.guessesUsed()).isEqualTo(0);
        assertThat(game.guessesRemaining()).isEqualTo(5);
    }

    @Test
    void startGame_gameIdIsAssigned() {
        var game = service.startGame();
        assertThat(game.gameId()).isNotBlank();
    }

    /**
     * Uses a FixedRandom that always returns 4 from nextInt, so secret = MIN + 4 = 5.
     * This ensures both "too low" (1–4) and "too high" (6–10) paths are testable without
     * hitting edge cases at range boundaries.
     */
    @Test
    void guess_tooLowFeedback() {
        var det = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var game = det.startGame();

        var result = det.guess(game.gameId(), 3);

        assertThat(result.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(result.lastFeedback()).containsIgnoringCase("low");
    }

    @Test
    void guess_tooHighFeedback() {
        var det = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var game = det.startGame();

        var result = det.guess(game.gameId(), 8);

        assertThat(result.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(result.lastFeedback()).containsIgnoringCase("high");
    }

    @Test
    void guess_correctNumberWins() {
        var det = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var game = det.startGame();

        var result = det.guess(game.gameId(), 5);

        assertThat(result.status()).isEqualTo(GameStatus.WON);
        assertThat(result.guessesUsed()).isEqualTo(1);
        assertThat(result.lastFeedback()).contains("1 guess");
    }

    @Test
    void guess_exhaustingAllGuessesWithoutWinningCausesLoss() {
        var det = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var game = det.startGame();

        Game lastResult = null;
        for (int i = 0; i < Game.MAX_GUESSES; i++) {
            lastResult = det.guess(game.gameId(), 1); // always wrong
        }

        assertThat(lastResult).isNotNull();
        assertThat(lastResult.status()).isEqualTo(GameStatus.LOST);
        assertThat(lastResult.guessesRemaining()).isEqualTo(0);
    }

    @Test
    void guess_unknownGameIdThrowsGameNotFoundException() {
        assertThatThrownBy(() -> service.guess("no-such-id", 5))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void guess_afterWinThrowsGameAlreadyEndedException() {
        var det = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var game = det.startGame();
        det.guess(game.gameId(), 5); // win

        assertThatThrownBy(() -> det.guess(game.gameId(), 1))
                .isInstanceOf(GameAlreadyEndedException.class);
    }

    @Test
    void guess_afterLossThrowsGameAlreadyEndedException() {
        var det = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var game = det.startGame();
        for (int i = 0; i < Game.MAX_GUESSES; i++) {
            det.guess(game.gameId(), 1); // exhaust guesses
        }

        assertThatThrownBy(() -> det.guess(game.gameId(), 5))
                .isInstanceOf(GameAlreadyEndedException.class);
    }

    @Test
    void guessesRemainingNeverDropsBelowZero() {
        var det = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var game = det.startGame();

        for (int i = 0; i < Game.MAX_GUESSES; i++) {
            det.guess(game.gameId(), 1);
        }

        var result = det.findGameResult(game.gameId()).orElseThrow();
        assertThat(result.guessesUsed()).isEqualTo(Game.MAX_GUESSES);
    }

    @Test
    void wonGame_isAccessibleViaFindGameResult() {
        var det = new GameService(new SimpleMeterRegistry(), new FixedRandom(4)); // secret = 5
        var game = det.startGame();
        det.guess(game.gameId(), 5);

        var result = det.findGameResult(game.gameId()).orElseThrow();

        assertThat(result.status()).isEqualTo(GameStatus.WON);
        assertThat(result.guessesUsed()).isEqualTo(1);
    }

}
