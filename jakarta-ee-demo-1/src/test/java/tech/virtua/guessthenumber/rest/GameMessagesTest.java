package tech.virtua.guessthenumber.rest;

import org.junit.jupiter.api.Test;
import tech.virtua.guessthenumber.game.GameSnapshot;
import tech.virtua.guessthenumber.game.GameStatus;
import tech.virtua.guessthenumber.game.Hint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMessagesTest {

    @Test
    void invitesThePlayerToStartBeforeTheFirstGuess() {
        String message = GameMessages.describe(
                new GameSnapshot(GameStatus.IN_PROGRESS, 0, 5, null, null, null));

        assertTrue(message.contains("between 1 and 10"), message);
        assertTrue(message.contains("5 guesses"), message);
    }

    @Test
    void pointsThePlayerLowerAfterAHighGuess() {
        String message = GameMessages.describe(
                new GameSnapshot(GameStatus.IN_PROGRESS, 2, 3, 9, Hint.TOO_HIGH, null));

        assertTrue(message.startsWith("9 is too high."), message);
        assertTrue(message.contains("lower"), message);
        assertTrue(message.contains("3 guesses left"), message);
    }

    @Test
    void pointsThePlayerHigherAfterALowGuess() {
        String message = GameMessages.describe(
                new GameSnapshot(GameStatus.IN_PROGRESS, 1, 4, 2, Hint.TOO_LOW, null));

        assertTrue(message.startsWith("2 is too low."), message);
        assertTrue(message.contains("higher"), message);
    }

    @Test
    void usesTheSingularOnTheLastGuess() {
        String message = GameMessages.describe(
                new GameSnapshot(GameStatus.IN_PROGRESS, 4, 1, 2, Hint.TOO_LOW, null));

        assertTrue(message.contains("1 guess left"), message);
    }

    @Test
    void reportsTheAnswerAndTheGuessCountOnAWin() {
        String message = GameMessages.describe(
                new GameSnapshot(GameStatus.WON, 3, 2, 7, Hint.CORRECT, 7));

        assertTrue(message.contains("thinking of 7"), message);
        assertTrue(message.contains("3 guesses"), message);
        assertTrue(message.contains("Play again"), message);
    }

    @Test
    void usesTheSingularForAFirstGuessWin() {
        String message = GameMessages.describe(
                new GameSnapshot(GameStatus.WON, 1, 4, 7, Hint.CORRECT, 7));

        assertTrue(message.contains("1 guess."), message);
    }

    @Test
    void revealsTheAnswerOnALoss() {
        String message = GameMessages.describe(
                new GameSnapshot(GameStatus.LOST, 5, 0, 3, Hint.TOO_LOW, 8));

        assertTrue(message.contains("Out of guesses"), message);
        assertTrue(message.contains("thinking of 8"), message);
        assertTrue(message.contains("Play again"), message);
    }

    @Test
    void saysRejectedInputDidNotCostAGuess() {
        String message = GameMessages.rejectInput(
                new GameSnapshot(GameStatus.IN_PROGRESS, 2, 3, 4, Hint.TOO_LOW, null));

        assertTrue(message.contains("whole number from 1 to 10"), message);
        assertTrue(message.contains("didn't count"), message);
        assertTrue(message.contains("3 guesses"), message);
    }

    @Test
    void neverLeavesThePlayerWithoutSomethingToRead() {
        for (GameStatus status : GameStatus.values()) {
            Integer secret = status == GameStatus.IN_PROGRESS ? null : 5;
            String message = GameMessages.describe(
                    new GameSnapshot(status, 1, 4, 5, Hint.TOO_LOW, secret));

            assertTrue(message != null && !message.isBlank(), "No message for " + status);
        }
    }

    @Test
    void buildsTheViewFromTheSnapshotAndTheGameRules() {
        GameSnapshot snapshot = new GameSnapshot(GameStatus.IN_PROGRESS, 2, 3, 9, Hint.TOO_HIGH, null);

        GameView view = GameView.of(snapshot);

        assertEquals(GameStatus.IN_PROGRESS, view.status());
        assertEquals(2, view.guessesMade());
        assertEquals(3, view.guessesRemaining());
        assertEquals(5, view.maxGuesses());
        assertEquals(1, view.minNumber());
        assertEquals(10, view.maxNumber());
        assertEquals(9, view.lastGuess().intValue());
        assertEquals(Hint.TOO_HIGH, view.lastHint());
        assertEquals(null, view.secretNumber());
        assertEquals(GameMessages.describe(snapshot), view.message());
    }
}
