package com.example.guessgame;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    @Test
    void newGameHasZeroGuessesAndIsNotOver() {
        var game = new Game();
        assertThat(game.getGuessCount()).isZero();
        assertThat(game.getGuessesRemaining()).isEqualTo(Game.getMaxGuesses());
        assertThat(game.isOver()).isFalse();
        assertThat(game.isWon()).isFalse();
        assertThat(game.isLost()).isFalse();
    }

    @Test
    void secretNumberIsInRange() {
        for (int i = 0; i < 200; i++) {
            var game = new Game();
            assertThat(game.getSecretNumber()).isBetween(1, 10);
        }
    }

    @Test
    void tooLowReturnedWhenGuessBelowSecret() {
        var game = gameWithSecret(7);
        assertThat(game.guess(3)).isEqualTo(GuessResult.TOO_LOW);
        assertThat(game.getGuessCount()).isEqualTo(1);
        assertThat(game.isOver()).isFalse();
    }

    @Test
    void tooHighReturnedWhenGuessAboveSecret() {
        var game = gameWithSecret(4);
        assertThat(game.guess(9)).isEqualTo(GuessResult.TOO_HIGH);
    }

    @Test
    void correctReturnedOnExactGuess() {
        var game = gameWithSecret(5);
        assertThat(game.guess(5)).isEqualTo(GuessResult.CORRECT);
        assertThat(game.isWon()).isTrue();
        assertThat(game.isOver()).isTrue();
    }

    @Test
    void gameOverReturnedWhenGuessLimitExceeded() {
        var game = gameWithSecret(10);
        // 5 wrong guesses
        for (int i = 0; i < 4; i++) {
            game.guess(1);
        }
        var result = game.guess(1);
        assertThat(result).isEqualTo(GuessResult.GAME_OVER);
        assertThat(game.isLost()).isTrue();
        assertThat(game.isOver()).isTrue();
    }

    @Test
    void guessesRemainingDecreasesCorrectly() {
        var game = gameWithSecret(10);
        int initial = game.getGuessesRemaining();
        game.guess(1); // wrong
        assertThat(game.getGuessesRemaining()).isEqualTo(initial - 1);
    }

    @Test
    void guessAfterGameOverReturnsGameOver() {
        var game = gameWithSecret(5);
        game.guess(5); // correct → won
        assertThat(game.guess(5)).isEqualTo(GuessResult.GAME_OVER);
    }

    @Test
    void guessesRemainingNeverGoesNegative() {
        var game = gameWithSecret(10);
        for (int i = 0; i < Game.getMaxGuesses() + 3; i++) {
            game.guess(1);
        }
        assertThat(game.getGuessesRemaining()).isGreaterThanOrEqualTo(0);
    }

    // ---- helper ----

    /**
     * Creates a Game whose secret number is forced via reflection for deterministic tests.
     */
    private Game gameWithSecret(int secret) {
        try {
            var game = new Game();
            var field = Game.class.getDeclaredField("secretNumber");
            field.setAccessible(true);
            field.set(game, secret);
            return game;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
