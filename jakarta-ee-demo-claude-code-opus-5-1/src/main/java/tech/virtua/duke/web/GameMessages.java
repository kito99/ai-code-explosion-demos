package tech.virtua.duke.web;

import tech.virtua.duke.game.GameSnapshot;
import tech.virtua.duke.game.SubmissionOutcome;

import static java.lang.String.format;
import static tech.virtua.duke.game.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.game.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.game.GameRules.LOWEST_NUMBER;

/// The plain, friendly sentence the player reads. Every answer carries one, so the page
/// never has to work out what just happened.
public final class GameMessages {

    private GameMessages() {
    }

    /// Describes a game, and what the submission that led to it did.
    public static String describe(final GameSnapshot game, final SubmissionOutcome outcome) {
        return switch (outcome) {
            case NONE -> standing(game);
            case COUNTED -> result(game);
            case NOT_A_VALID_NUMBER -> format(
                    "Guesses have to be whole numbers from %d to %d. That one did not count, so you still have %s.",
                    LOWEST_NUMBER, HIGHEST_NUMBER, guesses(game.guessesRemaining()));
            case GAME_ALREADY_OVER -> switch (game.status()) {
                case WON -> "You already found it. Start a new game to play again.";
                case LOST, ACTIVE -> "That game is over. Start a new game to play again.";
            };
        };
    }

    private static String standing(final GameSnapshot game) {
        if (game.status().isOver()) {
            return result(game);
        }
        if (game.guessesMade() == 0) {
            return format("Duke is thinking of a number from %d to %d. You have %s.",
                    LOWEST_NUMBER, HIGHEST_NUMBER, guesses(GUESS_LIMIT));
        }
        return format("Duke is still thinking of a number from %d to %d. You have %s left.",
                LOWEST_NUMBER, HIGHEST_NUMBER, guesses(game.guessesRemaining()));
    }

    private static String result(final GameSnapshot game) {
        return switch (game.status()) {
            case WON -> format("You got it! Duke was thinking of %d, and you found it in %s.",
                    secretNumberOf(game), guesses(game.guessesMade()));
            case LOST -> format("%d was too %s. Out of guesses — Duke was thinking of %d. Play again?",
                    lastGuessOf(game), hintFor(game).direction(), secretNumberOf(game));
            case ACTIVE -> {
                final var hint = hintFor(game);
                yield format("%d is too %s. Try something %s — %s left.",
                        lastGuessOf(game), hint.direction(), hint.nextTry(),
                        guesses(game.guessesRemaining()));
            }
        };
    }

    /// The two words a wrong guess needs: what it was, and where to go next.
    private record Hint(String direction, String nextTry) {
    }

    private static Hint hintFor(final GameSnapshot game) {
        return switch (game.feedback()) {
            case TOO_HIGH -> new Hint("high", "lower");
            case TOO_LOW -> new Hint("low", "higher");
            case NONE, CORRECT -> throw new IllegalStateException(
                    format("A guess that ended short of the number cannot have answered %s.", game.feedback()));
        };
    }

    private static String guesses(final int count) {
        return count == 1 ? "1 guess" : format("%d guesses", count);
    }

    private static int secretNumberOf(final GameSnapshot game) {
        return game.secretNumber().orElseThrow(
                () -> new IllegalStateException("A finished game must reveal its secret number."));
    }

    private static int lastGuessOf(final GameSnapshot game) {
        return game.lastGuess().orElseThrow(
                () -> new IllegalStateException("A counted guess must be remembered."));
    }
}
