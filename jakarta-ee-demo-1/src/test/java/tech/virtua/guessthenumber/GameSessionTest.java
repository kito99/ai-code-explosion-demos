package tech.virtua.guessthenumber;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.virtua.guessthenumber.game.Acceptance;
import tech.virtua.guessthenumber.game.GameSnapshot;
import tech.virtua.guessthenumber.game.GameStatus;
import tech.virtua.guessthenumber.game.GuessingGame;
import tech.virtua.guessthenumber.game.SubmitResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession();
    }

    @Test
    void startsAGameOnFirstRead() {
        GameSnapshot game = session.current();

        assertEquals(GameStatus.IN_PROGRESS, game.status());
        assertEquals(0, game.guessesMade());
        assertEquals(GuessingGame.MAX_GUESSES, game.guessesRemaining());
    }

    @Test
    void keepsTheSameGameAcrossReads() {
        session.submit("3");
        int guessesAfterOne = session.current().guessesMade();

        assertEquals(1, guessesAfterOne);
        assertEquals(1, session.current().guessesMade());
    }

    @Test
    void countsAValidGuess() {
        SubmitResult result = session.submit("5");

        assertEquals(Acceptance.ACCEPTED, result.acceptance());
        assertEquals(1, result.game().guessesMade());
        assertEquals(GuessingGame.MAX_GUESSES - 1, result.game().guessesRemaining());
    }

    @Test
    void rejectsInputThatIsNotAWholeNumberInRangeWithoutCostingAGuess() {
        for (String junk : new String[]{"", "   ", "0", "11", "-1", "abc", "3.5", null}) {
            SubmitResult result = session.submit(junk);

            assertEquals(Acceptance.INVALID_INPUT, result.acceptance(), "Should have rejected " + junk);
            assertEquals(0, result.game().guessesMade(), "Rejected input must not count");
            assertEquals(GuessingGame.MAX_GUESSES, result.game().guessesRemaining());
            assertNull(result.game().lastHint());
        }
    }

    @Test
    void refusesGuessesOnceTheGameIsOver() {
        playUntilOver();
        int guessesWhenFinished = session.current().guessesMade();

        SubmitResult result = session.submit("1");

        assertEquals(Acceptance.GAME_OVER, result.acceptance());
        assertEquals(guessesWhenFinished, result.game().guessesMade(),
                "A refused guess must not move the counter");
        assertNotNull(result.game().secretNumber(), "A finished game reveals the answer");
    }

    @Test
    void startsOverWithAFullSetOfGuesses() {
        session.submit("1");
        session.submit("2");

        GameSnapshot fresh = session.startNewGame();

        assertEquals(GameStatus.IN_PROGRESS, fresh.status());
        assertEquals(0, fresh.guessesMade());
        assertEquals(GuessingGame.MAX_GUESSES, fresh.guessesRemaining());
        assertNull(fresh.lastGuess(), "A new game clears the previous feedback");
        assertNull(fresh.lastHint());
        assertNull(fresh.secretNumber());
    }

    @Test
    void canStartOverFromAFinishedGame() {
        playUntilOver();

        assertEquals(GameStatus.IN_PROGRESS, session.startNewGame().status());
        assertEquals(Acceptance.ACCEPTED, session.submit("4").acceptance());
    }

    @Test
    void neverReportsMoreGuessesThanTheLimitAllows() {
        for (int i = 0; i < GuessingGame.MAX_GUESSES * 3; i++) {
            GameSnapshot game = session.submit(String.valueOf((i % 10) + 1)).game();

            assertTrue(game.guessesRemaining() >= 0);
            assertTrue(game.guessesMade() <= GuessingGame.MAX_GUESSES);
        }
    }

    /** Plays a full game out, whatever the secret turns out to be. */
    private void playUntilOver() {
        for (int guess = 1; guess <= GuessingGame.MAX_GUESSES; guess++) {
            session.submit(String.valueOf(guess));
        }
        assertTrue(session.current().status() != GameStatus.IN_PROGRESS);
    }
}
