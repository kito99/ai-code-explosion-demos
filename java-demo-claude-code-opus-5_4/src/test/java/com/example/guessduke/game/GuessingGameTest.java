package com.example.guessduke.game;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.guessduke.game.GameRules.MAX_GUESSES;
import static com.example.guessduke.game.GameStatus.ACTIVE;
import static com.example.guessduke.game.GameStatus.LOST;
import static com.example.guessduke.game.GameStatus.WON;
import static com.example.guessduke.game.GuessOutcome.CORRECT;
import static com.example.guessduke.game.GuessOutcome.TOO_HIGH;
import static com.example.guessduke.game.GuessOutcome.TOO_LOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class GuessingGameTest {

    private static final int SECRET = 7;

    /// Enough repetitions that an unsynchronised state machine fails reliably.
    private static final int CONCURRENT_ROUNDS = 300;

    @Test
    @DisplayName("a new round is active with every guess still available")
    void startsActiveWithAllGuesses() {
        final var game = new GuessingGame(SECRET);

        assertThat(game.status()).isEqualTo(ACTIVE);
        assertThat(game.guessesMade()).isZero();
        assertThat(game.guessesRemaining()).isEqualTo(MAX_GUESSES);
        assertThat(game.isScorePosted()).isFalse();
    }

    @Test
    @DisplayName("a guess below the secret number reports too low")
    void reportsTooLow() {
        final var game = new GuessingGame(SECRET);

        assertThat(game.submitGuess(SECRET - 1)).isEqualTo(TOO_LOW);
        assertThat(game.status()).isEqualTo(ACTIVE);
    }

    @Test
    @DisplayName("a guess above the secret number reports too high")
    void reportsTooHigh() {
        final var game = new GuessingGame(SECRET);

        assertThat(game.submitGuess(SECRET + 1)).isEqualTo(TOO_HIGH);
        assertThat(game.status()).isEqualTo(ACTIVE);
    }

    @Test
    @DisplayName("every accepted guess moves the tally, and the two counts always add up")
    void countsAcceptedGuesses() {
        final var game = new GuessingGame(SECRET);

        game.submitGuess(1);
        game.submitGuess(2);

        assertThat(game.guessesMade()).isEqualTo(2);
        assertThat(game.guessesRemaining()).isEqualTo(MAX_GUESSES - 2);
    }

    @Test
    @DisplayName("finding the secret number wins the round on the guess that found it")
    void winsOnCorrectGuess() {
        final var game = new GuessingGame(SECRET);

        game.submitGuess(1);

        assertThat(game.submitGuess(SECRET)).isEqualTo(CORRECT);
        assertThat(game.status()).isEqualTo(WON);
        assertThat(game.guessesMade()).isEqualTo(2);
    }

    @Test
    @DisplayName("using the last guess without finding the number loses the round")
    void losesAfterLastGuess() {
        final var game = new GuessingGame(SECRET);

        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            assertThat(game.status()).isEqualTo(ACTIVE);
            game.submitGuess(SECRET + 1);
        }

        assertThat(game.status()).isEqualTo(LOST);
        assertThat(game.guessesMade()).isEqualTo(MAX_GUESSES);
        assertThat(game.guessesRemaining()).isZero();
    }

    @Test
    @DisplayName("a finished round accepts no further guesses")
    void rejectsGuessesOnceFinished() {
        final var game = new GuessingGame(SECRET);
        game.submitGuess(SECRET);

        assertThatExceptionOfType(GameNotActiveException.class)
                .isThrownBy(() -> game.submitGuess(1));
        assertThat(game.guessesMade()).isEqualTo(1);
    }

    @ParameterizedTest(name = "a guess of {0} is outside the playable range")
    @ValueSource(ints = {0, -3, 11, 100})
    void rejectsUnplayableGuesses(final int guess) {
        final var game = new GuessingGame(SECRET);

        assertThatExceptionOfType(InvalidGuessException.class)
                .isThrownBy(() -> game.submitGuess(guess));
        assertThat(game.guessesMade()).isZero();
    }

    @ParameterizedTest(name = "a secret number of {0} is not playable")
    @ValueSource(ints = {0, 11})
    void rejectsUnplayableSecretNumbers(final int secretNumber) {
        assertThatIllegalArgumentException().isThrownBy(() -> new GuessingGame(secretNumber));
    }

    @Test
    @DisplayName("only the first claim gets the round's single posting slot")
    void claimsScorePostingOnce() {
        final var game = new GuessingGame(SECRET);
        game.submitGuess(SECRET);

        assertThat(game.claimScorePosting()).isTrue();
        assertThat(game.claimScorePosting()).isFalse();
        assertThat(game.isScorePosted()).isTrue();
    }

    @Test
    @DisplayName("a released posting slot can be claimed again")
    void releasesScorePosting() {
        final var game = new GuessingGame(SECRET);
        game.submitGuess(SECRET);
        game.claimScorePosting();

        game.releaseScorePosting();

        assertThat(game.isScorePosted()).isFalse();
        assertThat(game.claimScorePosting()).isTrue();
    }

    @Test
    @DisplayName("a lost round has no score to post")
    void refusesScorePostingForALostRound() {
        final var game = new GuessingGame(SECRET);
        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            game.submitGuess(SECRET + 1);
        }
        assertThat(game.status()).isEqualTo(LOST);

        assertThatExceptionOfType(GameNotActiveException.class).isThrownBy(game::claimScorePosting);
    }

    @Test
    @DisplayName("a round still in play has no score to post")
    void refusesScorePostingForAnActiveRound() {
        final var game = new GuessingGame(SECRET);

        assertThatExceptionOfType(GameNotActiveException.class).isThrownBy(game::claimScorePosting);
    }

    @Test
    @DisplayName("guesses arriving at once never push the round past its limit")
    void neverExceedsTheGuessLimitUnderConcurrency() throws InterruptedException {
        final var contenders = 8;
        for (var round = 0; round < CONCURRENT_ROUNDS; round++) {
            final var game = new GuessingGame(SECRET);
            final var start = new CountDownLatch(1);
            final var finished = new CountDownLatch(contenders);
            for (var contender = 0; contender < contenders; contender++) {
                final var wrongGuess = contender % 2 == 0 ? SECRET - 1 : SECRET + 1;
                Thread.ofVirtual().start(() -> {
                    try {
                        start.await();
                        game.submitGuess(wrongGuess);
                    } catch (final GameNotActiveException | InterruptedException _) {
                        // Most contenders lose the race once the round has settled.
                    } finally {
                        finished.countDown();
                    }
                });
            }
            start.countDown();
            finished.await();

            assertThat(game.guessesMade()).isLessThanOrEqualTo(MAX_GUESSES);
            assertThat(game.guessesRemaining()).isNotNegative();
            assertThat(game.status()).isEqualTo(LOST);
        }
    }
}
