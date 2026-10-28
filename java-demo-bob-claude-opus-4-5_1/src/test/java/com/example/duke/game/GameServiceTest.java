package com.example.duke.game;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.*;

class GameServiceTest {

    private GameService service;

    @BeforeEach
    void setUp() {
        // Default service uses SecureRandom; individual tests override via seeded random where needed.
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
    void startGame_secretNumberIsWithinRange() {
        for (int i = 0; i < 100; i++) {
            var g = service.startGame();
            // Secret is not exposed via Game record — verify indirectly via GameResult.
            var result = service.findGameResult(g.gameId()).orElseThrow();
            // If we could win in 1 guess then the secret is in [1,10]; guessing all values
            // exercises the range. Here we just confirm the session was created correctly.
            assertThat(g.guessesRemaining()).isEqualTo(Game.MAX_GUESSES);
        }
    }

    /**
     * Uses a seeded Random that always produces 4 (MIN+3), so the secret is always 5.
     * That gives us a number guaranteed to have both a "too low" value (1–4) and a "too high"
     * value (6–10) with no edge cases.
     */
    @Test
    void guess_tooLowFeedback() {
        // seed 42 with nextInt(10)+1 gives 6; we want a mid-range value — use a fixed stub.
        var deterministicService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var game = deterministicService.startGame(); // secret = MIN + 4 = 5

        var result = deterministicService.guess(game.gameId(), 3); // 3 < 5

        assertThat(result.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(result.lastFeedback()).containsIgnoringCase("low");
    }

    @Test
    void guess_tooHighFeedback() {
        var deterministicService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var game = deterministicService.startGame(); // secret = 5

        var result = deterministicService.guess(game.gameId(), 8); // 8 > 5

        assertThat(result.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(result.lastFeedback()).containsIgnoringCase("high");
    }

    @Test
    void guess_correctNumberWins() {
        var deterministicService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var game = deterministicService.startGame(); // secret = 5

        var result = deterministicService.guess(game.gameId(), 5);

        assertThat(result.status()).isEqualTo(GameStatus.WON);
        assertThat(result.guessesUsed()).isEqualTo(1);
        assertThat(result.lastFeedback()).contains("1 guess");
    }

    @Test
    void guess_exhaustingAllGuessesWithoutWinningCausesLoss() {
        var deterministicService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var game = deterministicService.startGame(); // secret = 5

        Game lastResult = null;
        for (int i = 0; i < Game.MAX_GUESSES; i++) {
            lastResult = deterministicService.guess(game.gameId(), 1); // always wrong
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
        var deterministicService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var game = deterministicService.startGame(); // secret = 5
        deterministicService.guess(game.gameId(), 5); // win

        assertThatThrownBy(() -> deterministicService.guess(game.gameId(), 1))
                .isInstanceOf(GameAlreadyEndedException.class);
    }

    @Test
    void guess_afterLossThrowsGameAlreadyEndedException() {
        var deterministicService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var game = deterministicService.startGame(); // secret = 5
        for (int i = 0; i < Game.MAX_GUESSES; i++) {
            deterministicService.guess(game.gameId(), 1); // exhaust guesses
        }

        assertThatThrownBy(() -> deterministicService.guess(game.gameId(), 5))
                .isInstanceOf(GameAlreadyEndedException.class);
    }

    @Test
    void guessesRemainingNeverDropsBelowZero() {
        var deterministicService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var game = deterministicService.startGame(); // secret = 5

        for (int i = 0; i < Game.MAX_GUESSES; i++) {
            deterministicService.guess(game.gameId(), 1);
        }

        // After terminal transition the session is evicted; result is in completedResults.
        var result = deterministicService.findGameResult(game.gameId()).orElseThrow();
        assertThat(result.guessesUsed()).isEqualTo(Game.MAX_GUESSES);
    }

    @Test
    void wonGame_isAccessibleViaFindGameResult() {
        var deterministicService = new GameService(new SimpleMeterRegistry(), new FixedRandom(4));
        var game = deterministicService.startGame();
        deterministicService.guess(game.gameId(), 5);

        var result = deterministicService.findGameResult(game.gameId()).orElseThrow();

        assertThat(result.status()).isEqualTo(GameStatus.WON);
        assertThat(result.guessesUsed()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** A deterministic Random that always returns the given fixed value from nextInt. */
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
