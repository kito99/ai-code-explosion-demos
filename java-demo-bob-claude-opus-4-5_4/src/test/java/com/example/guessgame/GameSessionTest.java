package com.example.guessgame;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    @Test
    void initialStateIsActive() {
        GameSession game = new GameSession();
        assertEquals(GameSession.Status.ACTIVE, game.getStatus());
        assertEquals(0, game.getGuessCount());
        assertEquals(5, game.getGuessesRemaining());
    }

    @Test
    void correctGuessSetsWonStatus() {
        GameSession game = new GameSession();
        int secret = game.getSecretNumber();
        String feedback = game.processGuess(secret);
        assertEquals(GameSession.Status.WON, game.getStatus());
        assertTrue(feedback.contains("Correct"));
        assertEquals(1, game.getGuessCount());
    }

    @Test
    void tooLowFeedbackForLowerGuess() {
        GameSession game = new GameSession();
        int secret = game.getSecretNumber();
        if (secret == 1) {
            // Can't go lower; skip this case
            return;
        }
        String feedback = game.processGuess(secret - 1);
        assertTrue(feedback.toLowerCase().contains("low"),
                "Expected 'low' feedback but got: " + feedback);
        assertEquals(GameSession.Status.ACTIVE, game.getStatus());
    }

    @Test
    void tooHighFeedbackForHigherGuess() {
        GameSession game = new GameSession();
        int secret = game.getSecretNumber();
        if (secret == 10) {
            return;
        }
        String feedback = game.processGuess(secret + 1);
        assertTrue(feedback.toLowerCase().contains("high"),
                "Expected 'high' feedback but got: " + feedback);
        assertEquals(GameSession.Status.ACTIVE, game.getStatus());
    }

    @Test
    void loseAfterFiveWrongGuesses() {
        GameSession game = new GameSession();
        int secret = game.getSecretNumber();
        int wrong = (secret == 1) ? 2 : 1;

        for (int i = 0; i < 5; i++) {
            game.processGuess(wrong);
        }

        assertEquals(GameSession.Status.LOST, game.getStatus());
        assertEquals(5, game.getGuessCount());
        assertEquals(0, game.getGuessesRemaining());
    }

    @Test
    void guessesRemainingNeverGoesNegative() {
        GameSession game = new GameSession();
        int secret = game.getSecretNumber();
        int wrong = (secret == 1) ? 2 : 1;

        for (int i = 0; i < 5; i++) {
            game.processGuess(wrong);
        }

        assertEquals(0, game.getGuessesRemaining());
    }

    @Test
    void noGuessesAcceptedAfterLoss() {
        GameSession game = new GameSession();
        int secret = game.getSecretNumber();
        int wrong = (secret == 1) ? 2 : 1;

        for (int i = 0; i < 5; i++) {
            game.processGuess(wrong);
        }

        assertEquals(GameSession.Status.LOST, game.getStatus());
        String response = game.processGuess(secret);
        assertEquals(5, game.getGuessCount()); // count must not increase
        assertEquals(GameSession.Status.LOST, game.getStatus());
    }

    @Test
    void resetRestoresInitialState() {
        GameSession game = new GameSession();
        int secret = game.getSecretNumber();
        int wrong = (secret == 1) ? 2 : 1;
        game.processGuess(wrong);
        game.reset();

        assertEquals(GameSession.Status.ACTIVE, game.getStatus());
        assertEquals(0, game.getGuessCount());
        assertEquals(5, game.getGuessesRemaining());
        assertNull(game.getLastFeedback());
    }

    @Test
    void secretNumberIsInRange() {
        for (int i = 0; i < 100; i++) {
            GameSession game = new GameSession();
            int secret = game.getSecretNumber();
            assertTrue(secret >= 1 && secret <= 10,
                    "Secret number out of range: " + secret);
        }
    }
}
