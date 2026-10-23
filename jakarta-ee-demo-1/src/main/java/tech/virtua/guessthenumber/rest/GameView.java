package tech.virtua.guessthenumber.rest;

import tech.virtua.guessthenumber.game.GameSnapshot;
import tech.virtua.guessthenumber.game.GameStatus;
import tech.virtua.guessthenumber.game.GuessingGame;
import tech.virtua.guessthenumber.game.Hint;

/**
 * The game as the browser sees it. Every endpoint returns this same shape, so the
 * page can render from one response no matter which call produced it.
 *
 * @param secretNumber the answer, present only once the game is over
 * @param message      friendly text covering what just happened and what to do next
 */
public record GameView(
        GameStatus status,
        int guessesMade,
        int guessesRemaining,
        int maxGuesses,
        int minNumber,
        int maxNumber,
        Integer lastGuess,
        Hint lastHint,
        Integer secretNumber,
        String message) {

    static GameView of(GameSnapshot game, String message) {
        return new GameView(
                game.status(),
                game.guessesMade(),
                game.guessesRemaining(),
                GuessingGame.MAX_GUESSES,
                GuessingGame.MIN_NUMBER,
                GuessingGame.MAX_NUMBER,
                game.lastGuess(),
                game.lastHint(),
                game.secretNumber(),
                message);
    }

    /** Renders the game with the message that describes its current state. */
    static GameView of(GameSnapshot game) {
        return of(game, GameMessages.describe(game));
    }
}
