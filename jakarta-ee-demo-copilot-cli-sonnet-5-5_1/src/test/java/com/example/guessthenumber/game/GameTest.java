package com.example.guessthenumber.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    private static final int SECRET = 7;

    private Game newGame() {
        return new Game(GameRules.STANDARD, SECRET);
    }

    @Test
    void startsActiveWithEveryGuessAvailable() {
        final var game = newGame();

        assertAll(
                () -> assertEquals(GameStatus.ACTIVE, game.status()),
                () -> assertEquals(0, game.guessesMade()),
                () -> assertEquals(GameRules.STANDARD.maxGuesses(), game.guessesRemaining()),
                () -> assertTrue(game.revealedSecretNumber().isEmpty()));
    }

    @Test
    void reportsAGuessBelowTheSecretAsTooLow() {
        final var game = newGame();

        assertEquals(GuessOutcome.TOO_LOW, game.guess(SECRET - 1));
        assertEquals(GameStatus.ACTIVE, game.status());
        assertEquals(1, game.guessesMade());
    }

    @Test
    void reportsAGuessAboveTheSecretAsTooHigh() {
        final var game = newGame();

        assertEquals(GuessOutcome.TOO_HIGH, game.guess(SECRET + 1));
        assertEquals(GameStatus.ACTIVE, game.status());
    }

    @Test
    void winsOnTheSecretNumberAndRevealsIt() {
        final var game = newGame();

        assertEquals(GuessOutcome.CORRECT, game.guess(SECRET));
        assertAll(
                () -> assertEquals(GameStatus.WON, game.status()),
                () -> assertEquals(1, game.guessesMade()),
                () -> assertEquals(SECRET, game.revealedSecretNumber().orElseThrow()));
    }

    @Test
    void losesAfterTheGuessLimitAndRevealsTheSecret() {
        final var game = newGame();

        wrongGuesses(game, GameRules.STANDARD.maxGuesses());

        assertAll(
                () -> assertEquals(GameStatus.LOST, game.status()),
                () -> assertEquals(GameRules.STANDARD.maxGuesses(), game.guessesMade()),
                () -> assertEquals(0, game.guessesRemaining()),
                () -> assertEquals(SECRET, game.revealedSecretNumber().orElseThrow()));
    }

    @Test
    void refusesGuessesOnceTheGameIsOver() {
        final var game = newGame();
        game.guess(SECRET);

        assertThrows(GameAlreadyOverException.class, () -> game.guess(SECRET));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3, 11, 1000})
    void refusesGuessesOutsideTheRangeWithoutSpendingOne(final int outOfRange) {
        final var game = newGame();

        assertThrows(InvalidGuessException.class, () -> game.guess(outOfRange));
        assertAll(
                () -> assertEquals(0, game.guessesMade()),
                () -> assertEquals(GameRules.STANDARD.maxGuesses(), game.guessesRemaining()),
                () -> assertEquals(GameStatus.ACTIVE, game.status()));
    }

    @Test
    void neverLetsTheRemainingCountGoBelowZero() {
        final var game = newGame();

        wrongGuesses(game, GameRules.STANDARD.maxGuesses());
        assertThrows(GameAlreadyOverException.class, () -> game.guess(SECRET - 1));

        assertEquals(0, game.guessesRemaining());
    }

    @Test
    void tracksWhetherTheWinHasBeenPosted() {
        final var game = newGame();
        game.guess(SECRET);

        assertFalse(game.isScorePosted());
        game.markScorePosted();
        assertTrue(game.isScorePosted());
    }

    @Test
    void rejectsASecretOutsideTheRules() {
        assertThrows(IllegalArgumentException.class, () -> new Game(GameRules.STANDARD, 11));
    }

    private static void wrongGuesses(final Game game, final int count) {
        for (var spent = 0; spent < count; spent++) {
            game.guess(SECRET == 1 ? 2 : 1);
        }
    }
}
