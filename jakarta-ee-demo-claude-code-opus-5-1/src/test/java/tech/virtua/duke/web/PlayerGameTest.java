package tech.virtua.duke.web;

import org.junit.jupiter.api.Test;
import tech.virtua.duke.game.SecretNumberPicker;

import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.virtua.duke.game.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.game.GameStatus.ACTIVE;
import static tech.virtua.duke.game.GameStatus.WON;
import static tech.virtua.duke.game.SubmissionOutcome.COUNTED;
import static tech.virtua.duke.game.SubmissionOutcome.GAME_ALREADY_OVER;
import static tech.virtua.duke.game.SubmissionOutcome.NOT_A_VALID_NUMBER;

class PlayerGameTest {

    private final PlayerGame playerGame = new PlayerGame(dealing(7, 2));

    @Test
    void dealsAGameTheFirstTimeTheSessionAsksForOne() {
        assertAll(
                () -> assertEquals(ACTIVE, playerGame.current().status()),
                () -> assertEquals(0, playerGame.current().guessesMade()));
    }

    @Test
    void keepsTheSameGameAcrossRequests() {
        playerGame.submit("3");
        assertEquals(1, playerGame.current().guessesMade());
    }

    @Test
    void dealsAFreshGameOnRequest() {
        playerGame.submit("3");
        final var restarted = playerGame.startNewGame();
        assertAll(
                () -> assertEquals(ACTIVE, restarted.status()),
                () -> assertEquals(0, restarted.guessesMade()),
                () -> assertEquals(GUESS_LIMIT, restarted.guessesRemaining()),
                () -> assertEquals(OptionalInt.empty(), restarted.lastGuess()));
    }

    @Test
    void dealsTheNextNumberWhenTheGameRestarts() {
        playerGame.current();
        playerGame.startNewGame();
        assertEquals(WON, playerGame.submit("2").snapshot().status());
    }

    @Test
    void dealsAGameWhenTheFirstThingASessionDoesIsGuess() {
        assertEquals(COUNTED, playerGame.submit("3").outcome());
    }

    @Test
    void takesNoGuessesOnceTheGameIsOver() {
        playerGame.submit("7");
        assertEquals(GAME_ALREADY_OVER, playerGame.submit("3").outcome());
    }

    @Test
    void leavesTheGameAloneWhenTheSubmissionIsNotAGuess() {
        playerGame.submit("3");
        final var rejected = playerGame.submit("");
        assertAll(
                () -> assertEquals(NOT_A_VALID_NUMBER, rejected.outcome()),
                () -> assertEquals(1, rejected.snapshot().guessesMade()));
    }

    @Test
    void holdsOneGamePerPlayer() {
        final var otherPlayer = new PlayerGame(dealing(5));
        playerGame.submit("7");
        assertAll(
                () -> assertEquals(WON, playerGame.current().status()),
                () -> assertEquals(ACTIVE, otherPlayer.current().status()));
    }

    /// Hands out the given numbers in order, repeating the last one once they run out.
    private static SecretNumberPicker dealing(final Integer... secretNumbers) {
        final var numbers = List.of(secretNumbers);
        final var draws = new AtomicInteger();
        return () -> numbers.get(Math.min(draws.getAndIncrement(), numbers.size() - 1));
    }
}
