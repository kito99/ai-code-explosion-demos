package com.example.duke.service;

import com.example.duke.model.GameState;
import com.example.duke.model.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService();
    }

    @Test
    void newGameCreatesActiveGameWithValidSecretAndFullGuesses() {
        var game = gameService.newGame();

        assertThat(game.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(game.secretNumber()).isBetween(GameState.MIN_NUMBER, GameState.MAX_NUMBER);
        assertThat(game.guessesUsed()).isZero();
        assertThat(game.guessesRemaining()).isEqualTo(GameState.MAX_GUESSES);
        assertThat(game.won()).isFalse();
    }

    @Test
    void guessTooLowReturnsTooLowMessage() {
        // force a state where secret = 7
        var game = stateWithSecret(7);

        var updated = gameService.guess(game, 3);

        assertThat(updated.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(updated.message()).containsIgnoringCase("low");
        assertThat(updated.guessesUsed()).isEqualTo(1);
        assertThat(updated.guessesRemaining()).isEqualTo(GameState.MAX_GUESSES - 1);
    }

    @Test
    void guessTooHighReturnsTooHighMessage() {
        var game = stateWithSecret(3);

        var updated = gameService.guess(game, 8);

        assertThat(updated.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(updated.message()).containsIgnoringCase("high");
    }

    @Test
    void correctGuessTransitionsToWonState() {
        var game = stateWithSecret(5);

        var updated = gameService.guess(game, 5);

        assertThat(updated.status()).isEqualTo(GameStatus.WON);
        assertThat(updated.won()).isTrue();
        assertThat(updated.guessesUsed()).isEqualTo(1);
        assertThat(updated.guessesRemaining()).isZero();
        assertThat(updated.message()).containsIgnoringCase("1 guess");
    }

    @Test
    void exhaustingAllGuessesTransitionsToLostState() {
        var game = stateWithSecret(5);

        // make 4 wrong guesses
        for (var i = 1; i <= 4; i++) {
            game = gameService.guess(game, 1); // 1 != 5, so always wrong
        }
        assertThat(game.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(game.guessesRemaining()).isEqualTo(1);

        // final wrong guess
        var finalState = gameService.guess(game, 1);
        assertThat(finalState.status()).isEqualTo(GameStatus.LOST);
        assertThat(finalState.guessesRemaining()).isZero();
        assertThat(finalState.message()).contains("5"); // secret revealed
    }

    @Test
    void invalidGuessBelowRangeDoesNotCountAsGuess() {
        var game = stateWithSecret(5);

        var updated = gameService.guess(game, 0);

        assertThat(updated.guessesUsed()).isZero();
        assertThat(updated.guessesRemaining()).isEqualTo(GameState.MAX_GUESSES);
        assertThat(updated.status()).isEqualTo(GameStatus.ACTIVE);
    }

    @Test
    void invalidGuessAboveRangeDoesNotCountAsGuess() {
        var game = stateWithSecret(5);

        var updated = gameService.guess(game, 11);

        assertThat(updated.guessesUsed()).isZero();
        assertThat(updated.guessesRemaining()).isEqualTo(GameState.MAX_GUESSES);
    }

    @Test
    void guessOnFinishedGameIsIgnored() {
        var wonGame = stateWithSecret(5, 1, 4, GameStatus.WON);

        var unchanged = gameService.guess(wonGame, 5);

        assertThat(unchanged).isEqualTo(wonGame);
    }

    @Test
    void winMessageIncludesGuessCountPlural() {
        var game = stateWithSecret(5, 2, 3, GameStatus.ACTIVE);

        var updated = gameService.guess(game, 5);

        assertThat(updated.message()).containsIgnoringCase("3 guesses");
    }

    // --- helpers ---

    private static GameState stateWithSecret(int secret) {
        return new GameState(secret, 0, GameState.MAX_GUESSES, GameStatus.ACTIVE, "", false);
    }

    private static GameState stateWithSecret(int secret, int guessesUsed, int guessesRemaining, GameStatus status) {
        return new GameState(secret, guessesUsed, guessesRemaining, status, "", false);
    }
}
