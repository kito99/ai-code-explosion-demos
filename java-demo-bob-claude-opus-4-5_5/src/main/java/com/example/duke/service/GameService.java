package com.example.duke.service;

import com.example.duke.model.GameState;
import com.example.duke.model.GameStatus;
import org.springframework.stereotype.Service;

import java.util.random.RandomGenerator;

/**
 * Handles game logic: starting a game and evaluating guesses.
 * Game state is carried by the client (via the session) — this service is stateless.
 */
@Service
public class GameService {

    private final RandomGenerator random = RandomGenerator.getDefault();

    /** Creates a fresh game with a new secret number. */
    public GameState newGame() {
        var secret = random.nextInt(GameState.MIN_NUMBER, GameState.MAX_NUMBER + 1);
        return new GameState(
                secret,
                0,
                GameState.MAX_GUESSES,
                GameStatus.ACTIVE,
                "I'm thinking of a number between 1 and 10. Can you guess it?",
                false
        );
    }

    /**
     * Evaluates one guess against the provided game state.
     *
     * @param currentState  the state sent back from the client
     * @param guess         the player's guess
     * @return the updated game state after the guess
     */
    public GameState guess(GameState currentState, int guess) {
        if (currentState.status() != GameStatus.ACTIVE) {
            return currentState;
        }

        if (guess < GameState.MIN_NUMBER || guess > GameState.MAX_NUMBER) {
            return new GameState(
                    currentState.secretNumber(),
                    currentState.guessesUsed(),
                    currentState.guessesRemaining(),
                    GameStatus.ACTIVE,
                    "Please enter a number between 1 and 10.",
                    false
            );
        }

        var newGuessesUsed = currentState.guessesUsed() + 1;
        var newGuessesRemaining = currentState.guessesRemaining() - 1;

        if (guess == currentState.secretNumber()) {
            return new GameState(
                    currentState.secretNumber(),
                    newGuessesUsed,
                    0,
                    GameStatus.WON,
                    "🎉 You got it! Duke's number was %d. You guessed it in %d %s!".formatted(
                            currentState.secretNumber(),
                            newGuessesUsed,
                            newGuessesUsed == 1 ? "guess" : "guesses"),
                    true
            );
        }

        if (newGuessesRemaining == 0) {
            return new GameState(
                    currentState.secretNumber(),
                    newGuessesUsed,
                    0,
                    GameStatus.LOST,
                    "No guesses left! Duke's number was %d. Better luck next time!".formatted(
                            currentState.secretNumber()),
                    false
            );
        }

        var hint = guess < currentState.secretNumber() ? "Too low!" : "Too high!";
        return new GameState(
                currentState.secretNumber(),
                newGuessesUsed,
                newGuessesRemaining,
                GameStatus.ACTIVE,
                "%s Try again — you have %d %s left.".formatted(
                        hint, newGuessesRemaining, newGuessesRemaining == 1 ? "guess" : "guesses"),
                false
        );
    }
}
