package tech.virtua.duke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toUnmodifiableSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.virtua.duke.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.GameRules.LOWEST_NUMBER;
import static tech.virtua.duke.GameStatus.ACTIVE;
import static tech.virtua.duke.GameStatus.LOST;
import static tech.virtua.duke.GameStatus.WON;
import static tech.virtua.duke.GuessFeedback.CORRECT;
import static tech.virtua.duke.GuessFeedback.NONE;
import static tech.virtua.duke.GuessFeedback.REJECTED;
import static tech.virtua.duke.GuessFeedback.TOO_HIGH;
import static tech.virtua.duke.GuessFeedback.TOO_LOW;

class GuessingGameTest {

    private static final int SECRET = 7;
    private static final int DRAW_COUNT = 10_000;

    private static GuessingGame gameWithSecret(final int secret) {
        return new GuessingGame(() -> secret);
    }

    @Test
    void startsActiveWithNoGuessesMade() {
        final var state = gameWithSecret(SECRET).state();

        assertEquals(ACTIVE, state.status());
        assertEquals(NONE, state.feedback());
        assertEquals(0, state.guessesMade());
        assertEquals(GUESS_LIMIT, state.guessesRemaining());
        assertTrue(state.lastGuess().isEmpty());
    }

    @Test
    void hidesTheSecretWhileTheGameIsActive() {
        assertTrue(gameWithSecret(SECRET).state().revealedSecretNumber().isEmpty());
    }

    @Test
    void randomPickerStaysInRangeAndReachesEveryNumber() {
        final var picker = SecretNumberPicker.random();
        final var drawn = IntStream.range(0, DRAW_COUNT)
                .map(_ -> picker.pickSecretNumber())
                .boxed()
                .collect(toUnmodifiableSet());

        assertTrue(drawn.stream().allMatch(GameRules::isInRange), () -> "out of range: " + drawn);
        assertEquals(HIGHEST_NUMBER - LOWEST_NUMBER + 1, drawn.size());
    }

    @Test
    void reportsAGuessAboveTheSecretAsTooHigh() {
        final var state = gameWithSecret(SECRET).submitGuess("9");

        assertEquals(TOO_HIGH, state.feedback());
        assertEquals(ACTIVE, state.status());
        assertEquals(1, state.guessesMade());
        assertEquals(GUESS_LIMIT - 1, state.guessesRemaining());
    }

    @Test
    void reportsAGuessBelowTheSecretAsTooLow() {
        final var state = gameWithSecret(SECRET).submitGuess("2");

        assertEquals(TOO_LOW, state.feedback());
        assertEquals(ACTIVE, state.status());
        assertEquals(1, state.guessesMade());
    }

    @Test
    void winsOnTheCorrectGuessAndRevealsTheSecret() {
        final var game = gameWithSecret(SECRET);
        game.submitGuess("3");
        final var state = game.submitGuess("7");

        assertEquals(WON, state.status());
        assertEquals(CORRECT, state.feedback());
        assertEquals(2, state.guessesMade());
        assertEquals(SECRET, state.revealedSecretNumber().orElseThrow());
    }

    @Test
    void losesAfterTheGuessLimitAndRevealsTheSecret() {
        final var game = gameWithSecret(SECRET);
        IntStream.range(0, GUESS_LIMIT).forEach(_ -> game.submitGuess("1"));
        final var state = game.state();

        assertEquals(LOST, state.status());
        assertEquals(GUESS_LIMIT, state.guessesMade());
        assertEquals(0, state.guessesRemaining());
        assertEquals(SECRET, state.revealedSecretNumber().orElseThrow());
    }

    @Test
    void ignoresGuessesOnceTheGameIsWon() {
        final var game = gameWithSecret(SECRET);
        final var won = game.submitGuess("7");
        final var afterLateGuess = game.submitGuess("1");

        assertEquals(won, afterLateGuess);
        assertEquals(WON, afterLateGuess.status());
    }

    @Test
    void ignoresGuessesOnceTheGameIsLost() {
        final var game = gameWithSecret(SECRET);
        IntStream.range(0, GUESS_LIMIT).forEach(_ -> game.submitGuess("1"));
        final var lost = game.state();

        assertEquals(lost, game.submitGuess("7"));
        assertEquals(LOST, game.state().status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "11", "-3", "3.5", "seven", "", "   ", "+4", "007 7", "999"})
    void rejectsAnythingThatIsNotAWholeNumberInRange(final String rawGuess) {
        final var state = gameWithSecret(SECRET).submitGuess(rawGuess);

        assertEquals(REJECTED, state.feedback());
        assertEquals(ACTIVE, state.status());
        assertEquals(0, state.guessesMade());
        assertEquals(GUESS_LIMIT, state.guessesRemaining());
    }

    @Test
    void acceptsAGuessWithSurroundingWhitespace() {
        assertEquals(CORRECT, gameWithSecret(SECRET).submitGuess("  7\n").feedback());
    }

    @Test
    void acceptsBothEndsOfTheRange() {
        assertEquals(CORRECT, gameWithSecret(LOWEST_NUMBER).submitGuess("1").feedback());
        assertEquals(CORRECT, gameWithSecret(HIGHEST_NUMBER).submitGuess("10").feedback());
    }

    @Test
    void rejectionKeepsTheEarlierFeedbackCountsIntact() {
        final var game = gameWithSecret(SECRET);
        game.submitGuess("2");
        final var state = game.submitGuess("nope");

        assertEquals(REJECTED, state.feedback());
        assertEquals(1, state.guessesMade());
        assertEquals(2, state.lastGuess().orElseThrow());
    }

    @Test
    void restartingClearsProgress() {
        final var game = gameWithSecret(SECRET);
        game.submitGuess("9");
        final var state = game.restart();

        assertEquals(ACTIVE, state.status());
        assertEquals(NONE, state.feedback());
        assertEquals(0, state.guessesMade());
        assertTrue(state.lastGuess().isEmpty());
    }

    @Test
    void restartingDrawsANewSecret() {
        final var nextSecret = new AtomicInteger(LOWEST_NUMBER);
        final var game = new GuessingGame(nextSecret::getAndIncrement);

        assertEquals(LOWEST_NUMBER, game.state().secretNumber());
        assertEquals(LOWEST_NUMBER + 1, game.restart().secretNumber());
        assertEquals(LOWEST_NUMBER + 2, game.restart().secretNumber());
    }

    @Test
    void restartingAfterALossMakesTheGamePlayableAgain() {
        final var game = gameWithSecret(SECRET);
        IntStream.range(0, GUESS_LIMIT).forEach(_ -> game.submitGuess("1"));
        game.restart();

        final var state = game.submitGuess("7");

        assertEquals(WON, state.status());
        assertEquals(1, state.guessesMade());
    }

    @Test
    void guessesRemainingNeverFallsBelowZero() {
        final var game = gameWithSecret(SECRET);
        IntStream.range(0, GUESS_LIMIT * 2).forEach(_ -> game.submitGuess("1"));

        assertEquals(0, game.state().guessesRemaining());
    }

    @Test
    void drawsAreIndependentSoConsecutiveGamesCanShareASecret() {
        final var game = new GuessingGame(SecretNumberPicker.random());
        var previousSecret = game.state().secretNumber();
        var repeated = false;

        for (var attempt = 0; attempt < DRAW_COUNT && !repeated; attempt++) {
            final var nextSecret = game.restart().secretNumber();
            repeated = nextSecret == previousSecret;
            previousSecret = nextSecret;
        }

        assertTrue(repeated, "no two consecutive games drew the same secret");
    }
}
