package tech.virtua.duke;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.virtua.duke.GameRules.GUESS_LIMIT;

class GameMessagesTest {

    private static final int SECRET = 7;

    private static GuessingGame gameWithSecret(final int secret) {
        return new GuessingGame(() -> secret);
    }

    @Test
    void invitesTheFirstGuess() {
        final var message = GameMessages.describe(gameWithSecret(SECRET).state());

        assertEquals("Duke is thinking of a number between 1 and 10. What is your guess?", message);
    }

    @Test
    void saysSoWhenAGuessIsTooHigh() {
        final var message = GameMessages.describe(gameWithSecret(SECRET).submitGuess("9"));

        assertEquals("9 is too high. Try lower. 4 guesses left.", message);
    }

    @Test
    void saysSoWhenAGuessIsTooLow() {
        final var message = GameMessages.describe(gameWithSecret(SECRET).submitGuess("2"));

        assertEquals("2 is too low. Try higher. 4 guesses left.", message);
    }

    @Test
    void usesTheSingularOnTheLastGuess() {
        final var game = gameWithSecret(SECRET);
        IntStream.range(0, GUESS_LIMIT - 1).forEach(_ -> game.submitGuess("1"));

        assertTrue(GameMessages.describe(game.state()).endsWith("1 guess left."));
    }

    @Test
    void explainsARejectedSubmissionWithoutRepeatingIt() {
        final var message = GameMessages.describe(gameWithSecret(SECRET).submitGuess("<script>"));

        assertEquals("That one does not count. Enter a whole number from 1 to 10.", message);
        assertFalse(message.contains("script"));
    }

    @Test
    void reportsHowManyGuessesTheWinTook() {
        final var game = gameWithSecret(SECRET);
        game.submitGuess("1");
        game.submitGuess("2");

        final var message = GameMessages.describe(game.submitGuess("7"));

        assertEquals("You got it! Duke was thinking of 7, and you found it in 3 guesses. Play again?", message);
    }

    @Test
    void usesTheSingularForAFirstTryWin() {
        final var message = GameMessages.describe(gameWithSecret(SECRET).submitGuess("7"));

        assertEquals("You got it! Duke was thinking of 7, and you found it in 1 guess. Play again?", message);
    }

    @Test
    void answersTheLastGuessAndThenRevealsTheSecret() {
        final var game = gameWithSecret(SECRET);
        IntStream.range(0, GUESS_LIMIT).forEach(_ -> game.submitGuess("1"));

        assertEquals("1 was too low, and that was your last guess. Duke was thinking of 7. Play again?",
                GameMessages.describe(game.state()));
    }

    @Test
    void answersALosingLastGuessThatWasTooHigh() {
        final var game = gameWithSecret(SECRET);
        IntStream.range(0, GUESS_LIMIT).forEach(_ -> game.submitGuess("9"));

        assertEquals("9 was too high, and that was your last guess. Duke was thinking of 7. Play again?",
                GameMessages.describe(game.state()));
    }
}
