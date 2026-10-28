package com.example.duke.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.duke.game.GameRules.MAX_GUESSES;
import static com.example.duke.game.GameStatus.IN_PROGRESS;
import static com.example.duke.game.GameStatus.LOST;
import static com.example.duke.game.GameStatus.WON;
import static com.example.duke.game.GuessOutcome.CORRECT;
import static com.example.duke.game.GuessOutcome.TOO_HIGH;
import static com.example.duke.game.GuessOutcome.TOO_LOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    private static final int SECRET = 7;

    @Test
    @DisplayName("a fresh game has no guesses and keeps the secret number hidden")
    void startsClean() {
        final var game = new Game(SECRET);

        assertEquals(IN_PROGRESS, game.status());
        assertEquals(0, game.guessesMade());
        assertEquals(MAX_GUESSES, game.guessesRemaining());
        assertTrue(game.lastGuess().isEmpty());
        assertTrue(game.lastOutcome().isEmpty());
        assertTrue(game.revealedSecretNumber().isEmpty());
        assertTrue(game.winningGuessCount().isEmpty());
    }

    @Test
    @DisplayName("a guess below the secret number is too low")
    void reportsTooLow() {
        final var game = new Game(SECRET);

        assertEquals(TOO_LOW, game.recordGuess(SECRET - 1));
        assertEquals(1, game.guessesMade());
        assertEquals(MAX_GUESSES - 1, game.guessesRemaining());
        assertEquals(IN_PROGRESS, game.status());
    }

    @Test
    @DisplayName("a guess above the secret number is too high")
    void reportsTooHigh() {
        final var game = new Game(SECRET);

        assertEquals(TOO_HIGH, game.recordGuess(SECRET + 1));
        assertEquals(IN_PROGRESS, game.status());
    }

    @Test
    @DisplayName("guessing the secret number wins and reports the guesses it took")
    void winsOnTheSecretNumber() {
        final var game = new Game(SECRET);
        game.recordGuess(1);

        assertEquals(CORRECT, game.recordGuess(SECRET));
        assertEquals(WON, game.status());
        assertEquals(2, game.winningGuessCount().orElseThrow());
        assertEquals(SECRET, game.revealedSecretNumber().orElseThrow());
    }

    @Test
    @DisplayName("using every guess loses and reveals the secret number")
    void losesWhenGuessesRunOut() {
        final var game = new Game(SECRET);

        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            game.recordGuess(SECRET + 1);
        }

        assertEquals(LOST, game.status());
        assertEquals(0, game.guessesRemaining());
        assertEquals(SECRET, game.revealedSecretNumber().orElseThrow());
        assertTrue(game.winningGuessCount().isEmpty());
    }

    @ParameterizedTest(name = "{0} is turned away without counting")
    @ValueSource(ints = {0, -3, 11, 99, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("a guess outside 1 to 10 does not count against the limit")
    void rejectsOutOfRangeGuesses(final int outOfRange) {
        final var game = new Game(SECRET);
        game.recordGuess(2);

        assertThrows(InvalidGuessException.class, () -> game.recordGuess(outOfRange));
        assertEquals(1, game.guessesMade());
        assertEquals(MAX_GUESSES - 1, game.guessesRemaining());
        assertEquals(IN_PROGRESS, game.status());
    }

    @Test
    @DisplayName("a won game takes no further guesses")
    void refusesGuessesAfterAWin() {
        final var game = new Game(SECRET);
        game.recordGuess(SECRET);

        assertThrows(GameAlreadyOverException.class, () -> game.recordGuess(1));
        assertEquals(1, game.guessesMade());
    }

    @Test
    @DisplayName("a lost game takes no further guesses")
    void refusesGuessesAfterALoss() {
        final var game = new Game(SECRET);
        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            game.recordGuess(SECRET - 1);
        }

        assertThrows(GameAlreadyOverException.class, () -> game.recordGuess(SECRET));
        assertEquals(MAX_GUESSES, game.guessesMade());
    }

    @Test
    @DisplayName("the last guess and its outcome are remembered")
    void remembersTheLastGuess() {
        final var game = new Game(SECRET);
        game.recordGuess(2);
        game.recordGuess(9);

        assertEquals(9, game.lastGuess().orElseThrow());
        assertEquals(TOO_HIGH, game.lastOutcome().orElseThrow());
    }

    @Test
    @DisplayName("a secret number outside the range is not a playable game")
    void refusesAnImpossibleSecretNumber() {
        assertThrows(InvalidGuessException.class, () -> new Game(0));
        assertThrows(InvalidGuessException.class, () -> new Game(11));
    }

    @Test
    @DisplayName("the status knows whether the round is over")
    void statusReportsWhetherTheRoundIsOver() {
        assertFalse(IN_PROGRESS.isOver());
        assertTrue(WON.isOver());
        assertTrue(LOST.isOver());
    }
}
