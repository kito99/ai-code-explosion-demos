package com.example.dukeguess.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.dukeguess.game.GameStatus.ACTIVE;
import static com.example.dukeguess.game.GameStatus.LOST;
import static com.example.dukeguess.game.GameStatus.WON;
import static com.example.dukeguess.game.GuessOutcome.CORRECT;
import static com.example.dukeguess.game.GuessOutcome.TOO_HIGH;
import static com.example.dukeguess.game.GuessOutcome.TOO_LOW;
import static com.example.dukeguess.game.GuessingGame.MAXIMUM_GUESSES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GuessingGameTest {

    private static final int SECRET_NUMBER = 7;

    private final GuessingGame game = new GuessingGame(SECRET_NUMBER);

    @Test
    void startsActiveWithEveryGuessAvailable() {
        assertThat(game.status()).isEqualTo(ACTIVE);
        assertThat(game.guessesMade()).isZero();
        assertThat(game.guessesRemaining()).isEqualTo(MAXIMUM_GUESSES);
        assertThat(game.revealedSecretNumber()).isEmpty();
    }

    @Test
    void reportsGuessesBelowTheSecretNumberAsTooLow() {
        assertThat(game.guess(SECRET_NUMBER - 1)).isEqualTo(TOO_LOW);
        assertThat(game.guessesMade()).isEqualTo(1);
        assertThat(game.guessesRemaining()).isEqualTo(MAXIMUM_GUESSES - 1);
    }

    @Test
    void reportsGuessesAboveTheSecretNumberAsTooHigh() {
        assertThat(game.guess(SECRET_NUMBER + 1)).isEqualTo(TOO_HIGH);
    }

    @Test
    void winsOnTheSecretNumberAndRevealsIt() {
        assertThat(game.guess(SECRET_NUMBER)).isEqualTo(CORRECT);
        assertThat(game.status()).isEqualTo(WON);
        assertThat(game.guessesMade()).isEqualTo(1);
        assertThat(game.revealedSecretNumber()).contains(SECRET_NUMBER);
    }

    @Test
    void winsOnTheLastAllowedGuessRatherThanRunningOut() {
        for (var attempt = 0; attempt < MAXIMUM_GUESSES - 1; attempt++) {
            game.guess(SECRET_NUMBER - 1);
        }

        assertThat(game.guess(SECRET_NUMBER)).isEqualTo(CORRECT);
        assertThat(game.status()).isEqualTo(WON);
        assertThat(game.guessesMade()).isEqualTo(MAXIMUM_GUESSES);
        assertThat(game.guessesRemaining()).isZero();
    }

    @Test
    void losesAfterTheGuessLimitAndRevealsTheSecretNumber() {
        for (var attempt = 0; attempt < MAXIMUM_GUESSES; attempt++) {
            game.guess(SECRET_NUMBER - 1);
        }

        assertThat(game.status()).isEqualTo(LOST);
        assertThat(game.guessesMade()).isEqualTo(MAXIMUM_GUESSES);
        assertThat(game.guessesRemaining()).isZero();
        assertThat(game.revealedSecretNumber()).contains(SECRET_NUMBER);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3, 11, 100})
    void rejectsGuessesOutsideTheRangeWithoutCountingThem(final int outOfRangeGuess) {
        assertThatExceptionOfType(GuessOutOfRangeException.class)
                .isThrownBy(() -> game.guess(outOfRangeGuess));

        assertThat(game.guessesMade()).isZero();
        assertThat(game.guessesRemaining()).isEqualTo(MAXIMUM_GUESSES);
        assertThat(game.status()).isEqualTo(ACTIVE);
    }

    @Test
    void refusesGuessesOnceTheGameIsWon() {
        game.guess(SECRET_NUMBER);

        assertThatExceptionOfType(GameAlreadyOverException.class)
                .isThrownBy(() -> game.guess(SECRET_NUMBER));
    }

    @Test
    void refusesGuessesOnceTheGameIsLost() {
        for (var attempt = 0; attempt < MAXIMUM_GUESSES; attempt++) {
            game.guess(SECRET_NUMBER + 1);
        }

        assertThatExceptionOfType(GameAlreadyOverException.class)
                .isThrownBy(() -> game.guess(SECRET_NUMBER));
    }

    @Test
    void rejectsSecretNumbersOutsideThePlayableRange() {
        assertThatExceptionOfType(GuessOutOfRangeException.class)
                .isThrownBy(() -> new GuessingGame(0));
    }

    @Test
    void startsFromTheSuppliedSecretNumber() {
        final SecretNumberGenerator fixedGenerator = (_, _) -> 3;

        assertThat(GuessingGame.startWith(fixedGenerator).guess(3)).isEqualTo(CORRECT);
    }
}
