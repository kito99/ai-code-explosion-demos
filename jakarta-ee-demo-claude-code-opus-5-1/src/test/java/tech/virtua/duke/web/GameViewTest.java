package tech.virtua.duke.web;

import org.junit.jupiter.api.Test;
import tech.virtua.duke.game.GuessingGame;
import tech.virtua.duke.game.SubmissionOutcome;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tech.virtua.duke.game.GameStatus.ACTIVE;
import static tech.virtua.duke.game.GameStatus.WON;
import static tech.virtua.duke.game.GuessFeedback.NONE;
import static tech.virtua.duke.game.GuessFeedback.TOO_LOW;
import static tech.virtua.duke.game.SubmissionOutcome.COUNTED;

class GameViewTest {

    private final GuessingGame fresh = GuessingGame.startWith(7);

    @Test
    void carriesTheRulesSoThePageNeverHasToKnowThem() {
        final var view = GameView.of(fresh.snapshot(), SubmissionOutcome.NONE);
        assertAll(
                () -> assertEquals(5, view.guessLimit()),
                () -> assertEquals(1, view.lowestNumber()),
                () -> assertEquals(10, view.highestNumber()));
    }

    @Test
    void holdsBackTheSecretNumberWhileTheGameIsBeingPlayed() {
        final var view = GameView.of(fresh.submit("3").snapshot(), COUNTED);
        assertAll(
                () -> assertEquals(ACTIVE, view.status()),
                () -> assertEquals(TOO_LOW, view.feedback()),
                () -> assertEquals(3, view.lastGuess()),
                () -> assertNull(view.secretNumber()));
    }

    @Test
    void givesUpTheSecretNumberOnceTheGameIsOver() {
        final var view = GameView.of(fresh.submit("7").snapshot(), COUNTED);
        assertAll(
                () -> assertEquals(WON, view.status()),
                () -> assertEquals(7, view.secretNumber()),
                () -> assertEquals(1, view.guessesMade()),
                () -> assertEquals(4, view.guessesRemaining()));
    }

    @Test
    void leavesTheLastGuessEmptyUntilOneLands() {
        final var view = GameView.of(fresh.snapshot(), SubmissionOutcome.NONE);
        assertAll(
                () -> assertNull(view.lastGuess()),
                () -> assertEquals(NONE, view.feedback()));
    }
}
