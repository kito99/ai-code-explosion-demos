package tech.virtua.duke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    @Test
    @DisplayName("FR-1: a new game starts in progress with a full set of guesses")
    void newGameStartsFresh() {
        GameState state = new Game(7).state();

        assertAll(
                () -> assertEquals(Game.Status.PLAYING, state.status()),
                () -> assertEquals(0, state.guessesMade()),
                () -> assertEquals(5, state.guessesRemaining()),
                () -> assertEquals(5, state.maxGuesses()),
                () -> assertEquals(1, state.minNumber()),
                () -> assertEquals(10, state.maxNumber()),
                () -> assertNull(state.lastGuess()),
                () -> assertNull(state.secret()),
                () -> assertTrue(state.message().contains("1 and 10")));
    }

    @Test
    @DisplayName("FR-2: the secret is always in range and every value can come up")
    void secretIsUniformOverTheRange() {
        Set<Integer> seen = new HashSet<>();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 2_000; i++) {
            int secret = new Game(random).secret();
            assertTrue(secret >= 1 && secret <= 10, "secret out of range: " + secret);
            seen.add(secret);
        }

        assertEquals(10, seen.size(), "expected every number from 1 to 10 to show up");
    }

    @Test
    @DisplayName("FR-7: a high guess reports too high and a low guess reports too low")
    void feedbackPointsInTheRightDirection() {
        Game game = new Game(5);

        GameState high = game.guess("9");
        assertEquals(Game.Hint.TOO_HIGH, high.hint());
        assertTrue(high.message().contains("too high"), high.message());

        GameState low = game.guess("2");
        assertEquals(Game.Hint.TOO_LOW, low.hint());
        assertTrue(low.message().contains("too low"), low.message());
    }

    @Test
    @DisplayName("FR-8: guesses made and remaining track accepted guesses")
    void countsTrackAcceptedGuesses() {
        Game game = new Game(5);

        GameState first = game.guess("1");
        assertEquals(1, first.guessesMade());
        assertEquals(4, first.guessesRemaining());

        GameState second = game.guess("2");
        assertEquals(2, second.guessesMade());
        assertEquals(3, second.guessesRemaining());
    }

    @Test
    @DisplayName("FR-9: a correct guess wins and reports how many guesses it took")
    void correctGuessWins() {
        Game game = new Game(4);
        game.guess("1");
        game.guess("2");

        GameState won = game.guess("4");

        assertAll(
                () -> assertEquals(Game.Status.WON, won.status()),
                () -> assertEquals(3, won.guessesMade()),
                () -> assertEquals(2, won.guessesRemaining()),
                () -> assertEquals(Game.Hint.NONE, won.hint()),
                () -> assertTrue(won.guessAccepted()),
                () -> assertTrue(won.message().contains("3 guesses"), won.message()));
    }

    @Test
    @DisplayName("FR-9: a first-try win says \"1 guess\", not \"1 guesses\"")
    void winOnFirstTryReadsNaturally() {
        GameState won = new Game(6).guess("6");

        assertTrue(won.message().contains("in 1 guess."), won.message());
    }

    @Test
    @DisplayName("FR-3 / FR-11: five wrong guesses lose the game and reveal the secret")
    void runningOutOfGuessesLoses() {
        Game game = new Game(10);

        for (int i = 1; i <= 4; i++) {
            GameState state = game.guess(String.valueOf(i));
            assertEquals(Game.Status.PLAYING, state.status(), "game ended early on guess " + i);
            assertNull(state.secret(), "secret leaked before the game ended");
        }

        GameState lost = game.guess("5");

        assertAll(
                () -> assertEquals(Game.Status.LOST, lost.status()),
                () -> assertEquals(5, lost.guessesMade()),
                () -> assertEquals(0, lost.guessesRemaining()),
                () -> assertEquals(Integer.valueOf(10), lost.secret()),
                () -> assertEquals(Game.Hint.TOO_LOW, lost.hint()),
                () -> assertTrue(lost.message().contains("too low"),
                        "FR-7 applies to the losing guess too: " + lost.message()),
                () -> assertTrue(lost.message().contains("thinking of 10"), lost.message()));
    }

    @Test
    @DisplayName("FR-9: the winning guess is accepted even when it is the last one")
    void winningOnTheLastGuessStillWins() {
        Game game = new Game(8);
        game.guess("1");
        game.guess("2");
        game.guess("3");
        game.guess("4");

        GameState won = game.guess("8");

        assertEquals(Game.Status.WON, won.status());
        assertEquals(0, won.guessesRemaining());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "0", "11", "42", "-3", "3.5", "abc", "seven", "5x", "1e1", "٣"})
    @DisplayName("FR-6: junk input is rejected without burning a guess")
    void invalidInputDoesNotCount(String raw) {
        Game game = new Game(5);
        game.guess("1");

        GameState state = game.guess(raw);

        assertAll(
                () -> assertEquals(Game.Status.PLAYING, state.status()),
                () -> assertEquals(1, state.guessesMade()),
                () -> assertEquals(4, state.guessesRemaining()),
                () -> assertTrue(state.message().contains("whole number"), state.message()),
                () -> assertTrue(state.message().contains("didn't count"), state.message()));
    }

    @Test
    @DisplayName("FR-6: surrounding whitespace is forgiven")
    void surroundingWhitespaceIsTrimmed() {
        GameState state = new Game(3).guess("  3  ");

        assertEquals(Game.Status.WON, state.status());
    }

    @Test
    @DisplayName("FR-9 / FR-11: guesses after the game ends are ignored")
    void guessesAfterTheGameEndsAreIgnored() {
        Game game = new Game(2);
        GameState won = game.guess("2");
        assertEquals(Game.Status.WON, won.status());

        GameState afterwards = game.guess("5");

        assertAll(
                () -> assertEquals(Game.Status.WON, afterwards.status()),
                () -> assertEquals(1, afterwards.guessesMade()),
                () -> assertTrue(afterwards.message().contains("Play again"), afterwards.message()));
    }

    @Test
    @DisplayName("Success criteria: guesses remaining never goes negative")
    void remainingNeverGoesNegative() {
        Game game = new Game(10);

        for (int i = 0; i < 20; i++) {
            GameState state = game.guess("1");
            assertTrue(state.guessesRemaining() >= 0, "remaining went negative");
        }
    }

    @Test
    @DisplayName("The state snapshot repeats the most recent message")
    void stateRepeatsTheLastMessage() {
        Game game = new Game(5);
        String afterGuess = game.guess("9").message();

        assertEquals(afterGuess, game.state().message());
        assertNotNull(game.state().lastGuess());
    }
}
