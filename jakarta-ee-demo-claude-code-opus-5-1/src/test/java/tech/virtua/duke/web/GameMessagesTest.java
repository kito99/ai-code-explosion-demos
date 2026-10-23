package tech.virtua.duke.web;

import org.junit.jupiter.api.Test;
import tech.virtua.duke.game.GameSnapshot;
import tech.virtua.duke.game.GuessingGame;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.virtua.duke.game.SubmissionOutcome.COUNTED;
import static tech.virtua.duke.game.SubmissionOutcome.GAME_ALREADY_OVER;
import static tech.virtua.duke.game.SubmissionOutcome.NONE;
import static tech.virtua.duke.game.SubmissionOutcome.NOT_A_VALID_NUMBER;

class GameMessagesTest {

    private static final int SECRET = 7;

    private final GuessingGame fresh = GuessingGame.startWith(SECRET);

    @Test
    void opensBySayingWhatTheGameIsAndHowManyGuessesThereAre() {
        assertEquals("Duke is thinking of a number from 1 to 10. You have 5 guesses.",
                GameMessages.describe(fresh.snapshot(), NONE));
    }

    @Test
    void pointsAGuessThatIsTooLowHigher() {
        assertEquals("3 is too low. Try something higher — 4 guesses left.",
                describe(fresh.submit("3").snapshot()));
    }

    @Test
    void pointsAGuessThatIsTooHighLower() {
        assertEquals("9 is too high. Try something lower — 4 guesses left.",
                describe(fresh.submit("9").snapshot()));
    }

    @Test
    void countsTheLastGuessInTheSingular() {
        final var oneLeft = fresh.submit("1").game().submit("2").game().submit("3").game().submit("4");
        assertEquals("4 is too low. Try something higher — 1 guess left.", describe(oneLeft.snapshot()));
    }

    @Test
    void celebratesAWinWithTheNumberAndTheCount() {
        assertEquals("You got it! Duke was thinking of 7, and you found it in 1 guess.",
                describe(fresh.submit("7").snapshot()));
    }

    @Test
    void countsSeveralGuessesInThePlural() {
        final var won = fresh.submit("1").game().submit("7");
        assertEquals("You got it! Duke was thinking of 7, and you found it in 2 guesses.",
                describe(won.snapshot()));
    }

    @Test
    void stillSaysHighOrLowOnTheGuessThatRunsTheClockOut() {
        assertEquals("1 was too low. Out of guesses — Duke was thinking of 7. Play again?",
                describe(lost().snapshot()));
    }

    @Test
    void saysHighOnALossToAGuessAboveTheNumber() {
        var game = fresh;
        for (var attempt = 0; attempt < 5; attempt++) {
            game = game.submit("9").game();
        }
        assertEquals("9 was too high. Out of guesses — Duke was thinking of 7. Play again?",
                describe(game.snapshot()));
    }

    @Test
    void explainsWhyASubmissionDidNotCount() {
        final var played = fresh.submit("3").game();
        assertEquals("Guesses have to be whole numbers from 1 to 10. "
                        + "That one did not count, so you still have 4 guesses.",
                GameMessages.describe(played.snapshot(), NOT_A_VALID_NUMBER));
    }

    @Test
    void pointsAFinishedGameAtTheNewGameButton() {
        final var won = fresh.submit("7").game();
        assertAll(
                () -> assertEquals("You already found it. Start a new game to play again.",
                        GameMessages.describe(won.snapshot(), GAME_ALREADY_OVER)),
                () -> assertEquals("That game is over. Start a new game to play again.",
                        GameMessages.describe(lost().snapshot(), GAME_ALREADY_OVER)));
    }

    @Test
    void remindsAReturningPlayerWhereTheyLeftOff() {
        final var played = fresh.submit("3").game();
        assertEquals("Duke is still thinking of a number from 1 to 10. You have 4 guesses left.",
                GameMessages.describe(played.snapshot(), NONE));
    }

    @Test
    void readsAFinishedGameTheSameWayWhetherOrNotAGuessJustLanded() {
        final var won = fresh.submit("7").game().snapshot();
        assertEquals(GameMessages.describe(won, COUNTED), GameMessages.describe(won, NONE));
    }

    private static String describe(final GameSnapshot snapshot) {
        return GameMessages.describe(snapshot, COUNTED);
    }

    private GuessingGame lost() {
        var game = fresh;
        for (var attempt = 0; attempt < 5; attempt++) {
            game = game.submit("1").game();
        }
        return game;
    }
}
