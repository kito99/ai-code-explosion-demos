package tech.virtua.guessthenumber.rest;

import tech.virtua.guessthenumber.game.GameSnapshot;
import tech.virtua.guessthenumber.game.GuessingGame;

/**
 * Turns game state into the one line of text the player reads. Every message says
 * what just happened and what the player can do next.
 */
final class GameMessages {

    private GameMessages() {
    }

    /** Describes where the game stands right now. */
    static String describe(GameSnapshot game) {
        return switch (game.status()) {
            case WON -> "You got it! Duke was thinking of " + game.secretNumber()
                    + ", and you found it in " + countOf(game.guessesMade(), "guess", "guesses")
                    + ". Play again for a new number.";
            case LOST -> "Out of guesses! Duke was thinking of " + game.secretNumber()
                    + ". Play again to take another run at it.";
            case IN_PROGRESS -> inProgress(game);
        };
    }

    /** Explains why a submission was thrown out without costing the player a guess. */
    static String rejectInput(GameSnapshot game) {
        return "Please enter a whole number from " + GuessingGame.MIN_NUMBER
                + " to " + GuessingGame.MAX_NUMBER + ". That one didn't count, so you still have "
                + countOf(game.guessesRemaining(), "guess", "guesses") + ".";
    }

    private static String inProgress(GameSnapshot game) {
        if (game.lastHint() == null) {
            return "Duke is thinking of a number between " + GuessingGame.MIN_NUMBER
                    + " and " + GuessingGame.MAX_NUMBER + ". You get "
                    + countOf(GuessingGame.MAX_GUESSES, "guess", "guesses")
                    + " — take your best shot!";
        }
        String direction = switch (game.lastHint()) {
            case TOO_HIGH -> " is too high. Try a lower number";
            case TOO_LOW -> " is too low. Try a higher number";
            // A correct guess ends the game, so it never reaches this branch.
            case CORRECT -> " is right";
        };
        return game.lastGuess() + direction + " — you have "
                + countOf(game.guessesRemaining(), "guess", "guesses") + " left.";
    }

    private static String countOf(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }
}
