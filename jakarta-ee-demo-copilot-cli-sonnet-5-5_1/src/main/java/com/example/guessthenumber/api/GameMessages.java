package com.example.guessthenumber.api;

import com.example.guessthenumber.game.Game;
import com.example.guessthenumber.game.GuessOutcome;

/// Builds the sentence that goes back to the player with every game response, so each
/// reply says what just happened and what to do next.
final class GameMessages {

    private GameMessages() {
    }

    static String forNewGame(final Game game) {
        return "Duke is thinking of a number between %d and %d. You have %s — good luck!".formatted(
                game.rules().lowestNumber(), game.rules().highestNumber(), guesses(game.guessesRemaining()));
    }

    /// The sentence for a game the player is returning to rather than acting on.
    static String forCurrentState(final Game game) {
        return switch (game.status()) {
            case ACTIVE -> game.guessesMade() == 0
                    ? forNewGame(game)
                    : "Duke is still thinking of a number between %d and %d. You have %s left.".formatted(
                            game.rules().lowestNumber(), game.rules().highestNumber(),
                            guesses(game.guessesRemaining()));
            case WON -> won(game);
            case LOST -> lost(game);
        };
    }

    static String forGuess(final int guess, final GuessOutcome outcome, final Game game) {
        if (outcome == GuessOutcome.CORRECT) {
            return won(game);
        }
        if (game.status().isFinished()) {
            return lost(game);
        }
        final var direction = outcome == GuessOutcome.TOO_LOW ? "low. Try a higher" : "high. Try a lower";
        return "%d is too %s number — you have %s left.".formatted(guess, direction,
                guesses(game.guessesRemaining()));
    }

    private static String won(final Game game) {
        return ("You got it! Duke was thinking of %d, and you found it in %s. "
                + "Add your name to post your score, or start a new game.").formatted(
                        secretNumber(game), guesses(game.guessesMade()));
    }

    private static String lost(final Game game) {
        return "Out of guesses! Duke was thinking of %d. Start a new game to try again."
                .formatted(secretNumber(game));
    }

    private static int secretNumber(final Game game) {
        return game.revealedSecretNumber()
                .orElseThrow(() -> new IllegalStateException("A finished game must reveal its secret number"));
    }

    private static String guesses(final int count) {
        return count == 1 ? "1 guess" : "%d guesses".formatted(count);
    }
}
