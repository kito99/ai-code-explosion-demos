package com.example.dukeguess.game;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.dukeguess.game.GameRules.MAX_GUESSES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GuessingGameTest {

    private static final int SECRET = 7;

    private final GuessingGame game = newGame(SECRET);

    private static GuessingGame newGame(final int secret) {
        return new GuessingGame(UUID.randomUUID(), secret, Instant.EPOCH);
    }

    @Test
    void startsInProgressWithEveryGuessAvailable() {
        assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(game.guessesMade()).isZero();
        assertThat(game.guessesRemaining()).isEqualTo(MAX_GUESSES);
        assertThat(game.revealedSecretNumber()).isEmpty();
    }

    @Test
    void tellsAGuessThatIsTooLow() {
        final var outcome = game.submitGuess(SECRET - 1);

        assertThat(outcome.verdict()).isEqualTo(GuessVerdict.TOO_LOW);
        assertThat(outcome.game().status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(outcome.game().guessesMade()).isEqualTo(1);
        assertThat(outcome.game().guessesRemaining()).isEqualTo(MAX_GUESSES - 1);
    }

    @Test
    void tellsAGuessThatIsTooHigh() {
        assertThat(game.submitGuess(SECRET + 1).verdict()).isEqualTo(GuessVerdict.TOO_HIGH);
    }

    @Test
    void winsOnTheSecretNumberAndRevealsIt() {
        final var outcome = game.submitGuess(SECRET);

        assertThat(outcome.verdict()).isEqualTo(GuessVerdict.CORRECT);
        assertThat(outcome.game().status()).isEqualTo(GameStatus.WON);
        assertThat(outcome.game().revealedSecretNumber()).contains(SECRET);
    }

    @Test
    void losesAfterTheLastGuessAndRevealsTheNumber() {
        final var wrong = SECRET - 1;
        for (var attempt = 1; attempt < MAX_GUESSES; attempt++) {
            assertThat(game.submitGuess(wrong).game().status()).isEqualTo(GameStatus.IN_PROGRESS);
        }

        final var lastOutcome = game.submitGuess(wrong);

        assertThat(lastOutcome.game().status()).isEqualTo(GameStatus.LOST);
        assertThat(lastOutcome.game().guessesRemaining()).isZero();
        assertThat(lastOutcome.game().revealedSecretNumber()).contains(SECRET);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3, 11, 99})
    void turnsAwayAGuessOutsideTheRangeWithoutSpendingOne(final int outOfRange) {
        assertThatExceptionOfType(InvalidGuessException.class)
                .isThrownBy(() -> game.submitGuess(outOfRange));

        assertThat(game.guessesMade()).isZero();
        assertThat(game.guessesRemaining()).isEqualTo(MAX_GUESSES);
        assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void takesNoFurtherGuessesOnceWon() {
        game.submitGuess(SECRET);

        assertThatExceptionOfType(GameAlreadyOverException.class)
                .isThrownBy(() -> game.submitGuess(SECRET));

        assertThat(game.guessesMade()).isEqualTo(1);
    }

    @Test
    void takesNoFurtherGuessesOnceLost() {
        final var lost = newGame(SECRET);
        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            lost.submitGuess(SECRET + 1);
        }

        assertThatExceptionOfType(GameAlreadyOverException.class)
                .isThrownBy(() -> lost.submitGuess(SECRET));
    }

    @Test
    void grantsTheScoreClaimOnlyOnce() {
        assertThat(game.claimScorePost()).isTrue();
        assertThat(game.claimScorePost()).isFalse();
    }

    @Test
    void grantsTheScoreClaimAgainAfterItIsHandedBack() {
        game.claimScorePost();

        game.releaseScorePost();

        assertThat(game.claimScorePost()).isTrue();
    }
}
