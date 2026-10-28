package com.example.guessgame.game;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.random.RandomGenerator;

/**
 * Mutable session-scoped game state, managed by the service layer.
 * Holds the secret number and accumulated guess count for one browser session.
 */
@Component
@SessionScope
class GameSessionState implements Serializable {

    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 10;

    private int secretNumber;
    private int guessCount;
    private GameState state;
    private GuessOutcome lastOutcome;
    private String lastFeedback;

    GameSessionState() {
        reset();
    }

    /** Resets this session to a freshly started game. */
    void reset() {
        secretNumber = RandomGenerator.getDefault().nextInt(MIN_NUMBER, MAX_NUMBER + 1);
        guessCount = 0;
        state = GameState.ACTIVE;
        lastOutcome = null;
        lastFeedback = "I'm thinking of a number between 1 and 10. Can you guess it?";
    }

    /**
     * Evaluates a guess and updates internal state.
     *
     * @param guess raw integer submitted by the player
     */
    void evaluate(int guess) {
        if (state != GameState.ACTIVE) {
            return;
        }
        if (guess < MIN_NUMBER || guess > MAX_NUMBER) {
            lastOutcome = GuessOutcome.INVALID;
            lastFeedback = "Please enter a whole number between 1 and 10.";
            return;
        }

        guessCount++;

        if (guess == secretNumber) {
            state = GameState.WON;
            lastOutcome = GuessOutcome.CORRECT;
            lastFeedback = "🎉 You got it in %d guess%s!".formatted(guessCount, guessCount == 1 ? "" : "es");
        } else if (guess < secretNumber) {
            lastOutcome = GuessOutcome.TOO_LOW;
            lastFeedback = "Too low! Try higher.";
            checkLoseCondition();
        } else {
            lastOutcome = GuessOutcome.TOO_HIGH;
            lastFeedback = "Too high! Try lower.";
            checkLoseCondition();
        }
    }

    private void checkLoseCondition() {
        if (guessCount >= GameSession.MAX_GUESSES && state == GameState.ACTIVE) {
            state = GameState.LOST;
            lastFeedback = "No guesses left! The number was %d.".formatted(secretNumber);
        }
    }

    GameSession toSnapshot() {
        var guessesLeft = Math.max(0, GameSession.MAX_GUESSES - guessCount);
        var revealedSecret = state == GameState.LOST ? secretNumber : null;
        return new GameSession(state, guessCount, guessesLeft, lastOutcome, revealedSecret, lastFeedback);
    }

    int getSecretNumber() {
        return secretNumber;
    }

    int getGuessCount() {
        return guessCount;
    }

    GameState getState() {
        return state;
    }
}
