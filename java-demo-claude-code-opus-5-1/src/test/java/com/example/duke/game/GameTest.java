package com.example.duke.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.example.duke.game.GameStatus.IN_PROGRESS;
import static com.example.duke.game.GameStatus.LOST;
import static com.example.duke.game.GameStatus.WON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameTest {

    private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");

    private static Game newGame(final int secretNumber) {
        return Game.start(UUID.randomUUID(), secretNumber, START);
    }

    @Test
    @DisplayName("a new game has no guesses and the full allowance")
    void startsEmpty() {
        final var game = newGame(7);

        assertThat(game.status()).isEqualTo(IN_PROGRESS);
        assertThat(game.guessesMade()).isZero();
        assertThat(game.guessesRemaining()).isEqualTo(GameRules.MAX_GUESSES);
        assertThat(game.revealedSecretNumber()).isEmpty();
        assertThat(game.scorePosted()).isFalse();
    }

    @Test
    @DisplayName("a wrong guess counts and leaves the game in progress")
    void countsWrongGuess() {
        final var game = newGame(7).withGuess(3);

        assertThat(game.status()).isEqualTo(IN_PROGRESS);
        assertThat(game.guessesMade()).isEqualTo(1);
        assertThat(game.guessesRemaining()).isEqualTo(GameRules.MAX_GUESSES - 1);
        assertThat(game.revealedSecretNumber()).isEmpty();
    }

    @Test
    @DisplayName("the right guess wins and reveals the number")
    void winsOnCorrectGuess() {
        final var game = newGame(7).withGuess(7);

        assertThat(game.status()).isEqualTo(WON);
        assertThat(game.guessesMade()).isEqualTo(1);
        assertThat(game.revealedSecretNumber()).contains(7);
    }

    @Test
    @DisplayName("the fifth wrong guess loses and reveals the number")
    void losesAfterFiveWrongGuesses() {
        var game = newGame(7);
        for (var attempt = 0; attempt < GameRules.MAX_GUESSES; attempt++) {
            game = game.withGuess(1);
        }

        assertThat(game.status()).isEqualTo(LOST);
        assertThat(game.guessesMade()).isEqualTo(GameRules.MAX_GUESSES);
        assertThat(game.guessesRemaining()).isZero();
        assertThat(game.revealedSecretNumber()).contains(7);
    }

    @Test
    @DisplayName("a correct fifth guess still wins")
    void winsOnLastGuess() {
        var game = newGame(7);
        for (var attempt = 0; attempt < GameRules.MAX_GUESSES - 1; attempt++) {
            game = game.withGuess(1);
        }

        assertThat(game.withGuess(7).status()).isEqualTo(WON);
    }

    @Test
    @DisplayName("a finished game takes no more guesses")
    void rejectsGuessAfterGameOver() {
        final var game = newGame(7).withGuess(7);

        assertThatThrownBy(() -> game.withGuess(2)).isInstanceOf(GameAlreadyFinishedException.class);
    }

    @Test
    @DisplayName("only a win can be posted, and only once")
    void guardsScorePosting() {
        final var won = newGame(7).withGuess(7);
        final var inProgress = newGame(7);

        assertThat(won.withScorePosted().scorePosted()).isTrue();
        assertThatThrownBy(() -> won.withScorePosted().withScorePosted())
                .isInstanceOf(ScoreAlreadyPostedException.class);
        assertThatThrownBy(inProgress::withScorePosted).isInstanceOf(GameNotWonException.class);
    }
}
