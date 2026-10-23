package tech.virtua.duke;

import static java.lang.String.format;
import static tech.virtua.duke.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.GameRules.LOWEST_NUMBER;
import static tech.virtua.duke.GameStatus.LOST;
import static tech.virtua.duke.GuessFeedback.TOO_HIGH;

/// The player-facing wording for a game state.
public final class GameMessages {

    private static final String INVITATION =
            format("Duke is thinking of a number between %d and %d. What is your guess?",
                    LOWEST_NUMBER, HIGHEST_NUMBER);

    private static final String REJECTION =
            format("That one does not count. Enter a whole number from %d to %d.",
                    LOWEST_NUMBER, HIGHEST_NUMBER);

    private GameMessages() {
    }

    /// The message to show for `state`, always saying what just happened and
    /// what the player can do next.
    public static String describe(final GameState state) {
        return switch (state.feedback()) {
            case NONE -> INVITATION;
            case REJECTED -> REJECTION;
            case CORRECT -> celebrate(state);
            case TOO_HIGH, TOO_LOW -> state.status() == LOST ? reveal(state) : nudge(state);
        };
    }

    private static String celebrate(final GameState state) {
        return format("You got it! Duke was thinking of %d, and you found it in %s. Play again?",
                state.secretNumber(), countOfGuesses(state.guessesMade()));
    }

    /// The last guess still gets its high or low answer before the secret comes
    /// out, so every guess the player makes is answered.
    private static String reveal(final GameState state) {
        return format("%d was %s, and that was your last guess. Duke was thinking of %d. Play again?",
                lastGuessOf(state), direction(state), state.secretNumber());
    }

    private static String nudge(final GameState state) {
        return format("%d is %s. Try %s. %s left.",
                lastGuessOf(state),
                direction(state),
                state.feedback() == TOO_HIGH ? "lower" : "higher",
                countOfGuesses(state.guessesRemaining()));
    }

    /// Only TOO_HIGH and TOO_LOW reach the callers of this, and both guarantee
    /// a last guess.
    private static int lastGuessOf(final GameState state) {
        return state.lastGuess().orElseThrow();
    }

    private static String direction(final GameState state) {
        return state.feedback() == TOO_HIGH ? "too high" : "too low";
    }

    private static String countOfGuesses(final int count) {
        return count == 1 ? "1 guess" : format("%d guesses", count);
    }
}
