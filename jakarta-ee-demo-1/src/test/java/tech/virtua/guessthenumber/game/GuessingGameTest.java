package tech.virtua.guessthenumber.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuessingGameTest {

    @Nested
    @DisplayName("Starting a game")
    class Starting {

        @Test
        void beginsWithEveryGuessAvailableAndNoFeedback() {
            GuessingGame game = new GuessingGame(7);

            assertEquals(GameStatus.IN_PROGRESS, game.status());
            assertEquals(0, game.guessesMade());
            assertEquals(GuessingGame.MAX_GUESSES, game.guessesRemaining());
            assertNull(game.lastGuess());
            assertNull(game.lastHint());
        }

        @Test
        void drawsRandomSecretsOnlyFromThePlayableRange() {
            RandomGenerator random = RandomGenerator.getDefault();
            Set<Integer> drawn = new HashSet<>();

            for (int i = 0; i < 2_000; i++) {
                int secret = GuessingGame.withRandomSecret(random).secretNumber();
                assertTrue(secret >= GuessingGame.MIN_NUMBER && secret <= GuessingGame.MAX_NUMBER,
                        "Secret out of range: " + secret);
                drawn.add(secret);
            }

            // Over this many draws every value should turn up, which catches an
            // off-by-one at either end of the range.
            assertEquals(GuessingGame.MAX_NUMBER - GuessingGame.MIN_NUMBER + 1, drawn.size());
        }

        @Test
        void refusesASecretOutsideTheRange() {
            assertThrows(IllegalArgumentException.class, () -> new GuessingGame(0));
            assertThrows(IllegalArgumentException.class, () -> new GuessingGame(11));
        }
    }

    @Nested
    @DisplayName("Reading player input")
    class Parsing {

        @ParameterizedTest
        @ValueSource(strings = {"1", "5", "10", " 7 ", "\t3\n"})
        void acceptsWholeNumbersInRange(String input) {
            assertTrue(GuessingGame.parseGuess(input).isPresent(), "Should have accepted " + input);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   ", "0", "11", "-3", "+7", "3.5", "1e1", "seven", "7a", "٧"})
        void rejectsAnythingElse(String input) {
            assertEquals(OptionalInt.empty(), GuessingGame.parseGuess(input),
                    "Should have rejected " + input);
        }

        @Test
        void rejectsNumbersTooLargeToHold() {
            assertEquals(OptionalInt.empty(), GuessingGame.parseGuess("99999999999999999999"));
        }

        @Test
        void keepsTheParsedValue() {
            assertEquals(8, GuessingGame.parseGuess(" 8 ").getAsInt());
        }
    }

    @Nested
    @DisplayName("Guessing")
    class Guessing {

        @Test
        void tellsThePlayerWhenAGuessIsTooHigh() {
            GuessingGame game = new GuessingGame(4);

            GuessOutcome outcome = game.guess(9);

            assertEquals(Hint.TOO_HIGH, outcome.hint());
            assertEquals(GameStatus.IN_PROGRESS, outcome.status());
        }

        @Test
        void tellsThePlayerWhenAGuessIsTooLow() {
            GuessingGame game = new GuessingGame(4);

            assertEquals(Hint.TOO_LOW, game.guess(2).hint());
        }

        @Test
        void countsEachAcceptedGuessAgainstTheLimit() {
            GuessingGame game = new GuessingGame(4);

            game.guess(1);
            assertEquals(1, game.guessesMade());
            assertEquals(4, game.guessesRemaining());

            game.guess(2);
            assertEquals(2, game.guessesMade());
            assertEquals(3, game.guessesRemaining());
        }

        @Test
        void refusesAGuessOutsideTheRange() {
            GuessingGame game = new GuessingGame(4);

            assertThrows(IllegalArgumentException.class, () -> game.guess(0));
            assertThrows(IllegalArgumentException.class, () -> game.guess(11));
            assertEquals(0, game.guessesMade(), "A refused guess must not cost the player anything");
        }
    }

    @Nested
    @DisplayName("Winning")
    class Winning {

        @Test
        void endsTheGameAndReportsTheGuessCount() {
            GuessingGame game = new GuessingGame(6);

            game.guess(1);
            game.guess(10);
            GuessOutcome outcome = game.guess(6);

            assertEquals(Hint.CORRECT, outcome.hint());
            assertEquals(GameStatus.WON, game.status());
            assertTrue(game.isOver());
            assertEquals(3, game.guessesMade());
        }

        @Test
        void winningOnTheLastGuessCountsAsAWinNotALoss() {
            GuessingGame game = new GuessingGame(6);

            game.guess(1);
            game.guess(2);
            game.guess(3);
            game.guess(4);
            assertEquals(GameStatus.IN_PROGRESS, game.status());

            assertEquals(GameStatus.WON, game.guess(6).status());
            assertEquals(0, game.guessesRemaining());
        }

        @Test
        void takesNoFurtherGuesses() {
            GuessingGame game = new GuessingGame(6);
            game.guess(6);

            assertThrows(IllegalStateException.class, () -> game.guess(5));
            assertEquals(1, game.guessesMade());
        }
    }

    @Nested
    @DisplayName("Losing")
    class Losing {

        @Test
        void endsAfterTheLastGuessIsSpent() {
            GuessingGame game = new GuessingGame(6);

            for (int guess = 1; guess <= GuessingGame.MAX_GUESSES; guess++) {
                assertEquals(6 == guess ? Hint.CORRECT : Hint.TOO_LOW, game.guess(guess).hint());
            }

            assertEquals(GameStatus.LOST, game.status());
            assertTrue(game.isOver());
            assertEquals(GuessingGame.MAX_GUESSES, game.guessesMade());
            assertEquals(0, game.guessesRemaining());
        }

        @Test
        void takesNoFurtherGuesses() {
            GuessingGame game = new GuessingGame(6);
            for (int guess = 1; guess <= GuessingGame.MAX_GUESSES; guess++) {
                game.guess(guess);
            }

            assertThrows(IllegalStateException.class, () -> game.guess(6));
            assertEquals(GuessingGame.MAX_GUESSES, game.guessesMade());
        }

        @Test
        void neverLetsGuessesRemainingGoNegative() {
            GuessingGame game = new GuessingGame(6);
            for (int guess = 1; guess <= GuessingGame.MAX_GUESSES; guess++) {
                game.guess(guess);
                assertTrue(game.guessesRemaining() >= 0);
            }
            assertEquals(0, game.guessesRemaining());
        }
    }

    @Nested
    @DisplayName("Snapshots")
    class Snapshots {

        @Test
        void withholdTheSecretWhileTheGameIsStillOn() {
            GuessingGame game = new GuessingGame(6);
            game.guess(2);

            GameSnapshot snapshot = game.snapshot();

            assertNull(snapshot.secretNumber(), "An in-progress game must not leak the answer");
            assertEquals(2, snapshot.lastGuess().intValue());
            assertEquals(Hint.TOO_LOW, snapshot.lastHint());
            assertEquals(4, snapshot.guessesRemaining());
        }

        @Test
        void revealTheSecretOnceTheGameIsOver() {
            GuessingGame game = new GuessingGame(6);
            for (int guess = 1; guess <= GuessingGame.MAX_GUESSES; guess++) {
                game.guess(guess);
            }

            assertEquals(6, game.snapshot().secretNumber().intValue());
        }

        @Test
        void doNotChangeWhenTheGameMovesOn() {
            GuessingGame game = new GuessingGame(6);
            game.guess(1);
            GameSnapshot taken = game.snapshot();

            game.guess(2);

            assertEquals(1, taken.guessesMade(), "A snapshot must not track later guesses");
            assertEquals(2, game.guessesMade());
        }

        @Test
        void reportNoFeedbackBeforeTheFirstGuess() {
            GameSnapshot snapshot = new GuessingGame(6).snapshot();

            assertNull(snapshot.lastGuess());
            assertNull(snapshot.lastHint());
            assertFalse(new GuessingGame(6).isOver());
        }
    }
}
