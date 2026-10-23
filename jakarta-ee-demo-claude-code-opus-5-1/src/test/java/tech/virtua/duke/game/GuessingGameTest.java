package tech.virtua.duke.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.virtua.duke.game.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.game.GameStatus.ACTIVE;
import static tech.virtua.duke.game.GameStatus.LOST;
import static tech.virtua.duke.game.GameStatus.WON;
import static tech.virtua.duke.game.GuessFeedback.CORRECT;
import static tech.virtua.duke.game.GuessFeedback.NONE;
import static tech.virtua.duke.game.GuessFeedback.TOO_HIGH;
import static tech.virtua.duke.game.GuessFeedback.TOO_LOW;
import static tech.virtua.duke.game.SubmissionOutcome.COUNTED;
import static tech.virtua.duke.game.SubmissionOutcome.GAME_ALREADY_OVER;
import static tech.virtua.duke.game.SubmissionOutcome.NOT_A_VALID_NUMBER;

class GuessingGameTest {

    private static final int SECRET = 7;

    private final GuessingGame fresh = GuessingGame.startWith(SECRET);

    @Nested
    class AtTheStart {

        @Test
        void isActiveWithEveryGuessStillToCome() {
            final var snapshot = fresh.snapshot();
            assertAll(
                    () -> assertEquals(ACTIVE, snapshot.status()),
                    () -> assertEquals(NONE, snapshot.feedback()),
                    () -> assertEquals(0, snapshot.guessesMade()),
                    () -> assertEquals(GUESS_LIMIT, snapshot.guessesRemaining()),
                    () -> assertEquals(OptionalInt.empty(), snapshot.lastGuess()));
        }

        @Test
        void keepsTheSecretNumberToItself() {
            assertEquals(OptionalInt.empty(), fresh.snapshot().secretNumber());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 11, -1, Integer.MAX_VALUE})
        void refusesASecretNumberNoOneCouldGuess(final int secretNumber) {
            assertThrows(SecretNumberOutOfRangeException.class, () -> GuessingGame.startWith(secretNumber));
        }

        @Test
        void takesItsSecretNumberFromThePicker() {
            assertEquals(4, GuessingGame.dealtBy(() -> 4).secretNumber());
        }
    }

    @Nested
    class WhileBeingPlayed {

        @Test
        void saysWhenAGuessIsTooLow() {
            final var after = fresh.submit("3");
            assertAll(
                    () -> assertEquals(COUNTED, after.outcome()),
                    () -> assertEquals(TOO_LOW, after.snapshot().feedback()),
                    () -> assertEquals(ACTIVE, after.snapshot().status()),
                    () -> assertEquals(1, after.snapshot().guessesMade()),
                    () -> assertEquals(GUESS_LIMIT - 1, after.snapshot().guessesRemaining()),
                    () -> assertEquals(OptionalInt.of(3), after.snapshot().lastGuess()));
        }

        @Test
        void saysWhenAGuessIsTooHigh() {
            assertEquals(TOO_HIGH, fresh.submit("9").snapshot().feedback());
        }

        @Test
        void countsEveryGuessThatLands() {
            final var after = fresh.submit("1").game().submit("2").game().submit("3");
            assertEquals(3, after.snapshot().guessesMade());
        }

        @Test
        void leavesTheGameAloneWhenTheSubmissionIsNotAGuess() {
            final var played = fresh.submit("3").game();
            final var after = played.submit("eleven");
            assertAll(
                    () -> assertEquals(NOT_A_VALID_NUMBER, after.outcome()),
                    () -> assertSame(played, after.game()),
                    () -> assertEquals(1, after.snapshot().guessesMade()),
                    () -> assertEquals(TOO_LOW, after.snapshot().feedback()));
        }

        @Test
        void neverRunsTheRemainingCountBelowZero() {
            var game = fresh;
            for (var attempt = 0; attempt < GUESS_LIMIT + 3; attempt++) {
                game = game.submit("1").game();
            }
            assertEquals(0, game.guessesRemaining());
        }
    }

    @Nested
    class OnWinning {

        @Test
        void congratulatesAndRevealsTheNumber() {
            final var after = fresh.submit("3").game().submit(String.valueOf(SECRET));
            assertAll(
                    () -> assertEquals(WON, after.snapshot().status()),
                    () -> assertEquals(CORRECT, after.snapshot().feedback()),
                    () -> assertEquals(2, after.snapshot().guessesMade()),
                    () -> assertEquals(OptionalInt.of(SECRET), after.snapshot().secretNumber()));
        }

        @Test
        void countsAWinOnTheLastGuessAsAWin() {
            var game = fresh;
            for (var attempt = 0; attempt < GUESS_LIMIT - 1; attempt++) {
                game = game.submit("1").game();
            }
            assertEquals(WON, game.submit(String.valueOf(SECRET)).snapshot().status());
        }

        @Test
        void takesNoMoreGuesses() {
            final var won = fresh.submit(String.valueOf(SECRET)).game();
            final var after = won.submit("4");
            assertAll(
                    () -> assertEquals(GAME_ALREADY_OVER, after.outcome()),
                    () -> assertSame(won, after.game()),
                    () -> assertEquals(1, after.snapshot().guessesMade()));
        }
    }

    @Nested
    class OnLosing {

        private final GuessingGame lost = playOut("1");

        @Test
        void endsTheGameAndRevealsTheNumber() {
            assertAll(
                    () -> assertEquals(LOST, lost.status()),
                    () -> assertEquals(GUESS_LIMIT, lost.snapshot().guessesMade()),
                    () -> assertEquals(0, lost.snapshot().guessesRemaining()),
                    () -> assertEquals(OptionalInt.of(SECRET), lost.snapshot().secretNumber()));
        }

        @Test
        void takesNoMoreGuesses() {
            final var after = lost.submit(String.valueOf(SECRET));
            assertAll(
                    () -> assertEquals(GAME_ALREADY_OVER, after.outcome()),
                    () -> assertEquals(LOST, after.snapshot().status()));
        }

        @Test
        @DisplayName("submissions that are not guesses never run the clock out")
        void nonGuessesNeverRunTheClockOut() {
            var game = GuessingGame.startWith(SECRET);
            for (var attempt = 0; attempt < 20; attempt++) {
                game = game.submit("nope").game();
            }
            final var unchanged = game;
            assertAll(
                    () -> assertEquals(ACTIVE, unchanged.status()),
                    () -> assertEquals(GUESS_LIMIT, unchanged.guessesRemaining()));
        }

        private GuessingGame playOut(final String wrongGuess) {
            var game = GuessingGame.startWith(SECRET);
            for (var attempt = 0; attempt < GUESS_LIMIT; attempt++) {
                game = game.submit(wrongGuess).game();
            }
            return game;
        }
    }
}
