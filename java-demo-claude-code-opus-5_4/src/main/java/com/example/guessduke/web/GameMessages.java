package com.example.guessduke.web;

import com.example.guessduke.game.GuessOutcome;
import com.example.guessduke.leaderboard.PlayerName;

import static com.example.guessduke.game.GameRules.HIGHEST_NUMBER;
import static com.example.guessduke.game.GameRules.LOWEST_NUMBER;
import static com.example.guessduke.game.GameRules.MAX_GUESSES;
import static com.example.guessduke.game.GuessOutcome.TOO_LOW;
import static com.example.guessduke.leaderboard.LeaderboardRules.MAX_NAME_LENGTH;

/// The plain-language text the player reads. Every message says what just happened
/// and what the player can do next.
public final class GameMessages {

    public static final String NEW_GAME =
            "Duke is thinking of a number between %d and %d. You get %d guesses — what's your first?"
                    .formatted(LOWEST_NUMBER, HIGHEST_NUMBER, MAX_GUESSES);

    public static final String SESSION_RESTARTED =
            "Your game had timed out, so Duke picked a fresh number. That guess didn't count — go ahead and guess again.";

    public static final String GUESS_NOT_ACCEPTED =
            "Guesses need to be a whole number from %d to %d. That one didn't count, so try another."
                    .formatted(LOWEST_NUMBER, HIGHEST_NUMBER);

    public static final String ROUND_ALREADY_OVER = "This round is over. Start a new game whenever you're ready.";

    public static final String NAME_NOT_ACCEPTED =
            "Duke needs a name of 1 to %d characters to credit the win. Try another one.".formatted(MAX_NAME_LENGTH);

    public static final String NO_SCORE_TO_POST = "There's nothing to post yet. Win a round and Duke will ask for your name.";

    public static final String SCORE_ALREADY_POSTED =
            "This win is already on the leaderboard. Start a new game to earn another spot.";

    private GameMessages() {
    }

    /// Tells the player which way to move after a wrong guess that left them guesses.
    public static String missed(final int guess, final GuessOutcome outcome, final int guessesRemaining) {
        return "%d is too %s. Aim %s — %s left.".formatted(guess, tooWhat(outcome), whichWay(outcome), guesses(guessesRemaining));
    }

    /// Congratulates the player and points them at the leaderboard.
    public static String won(final int secretNumber, final int guessesMade) {
        return "You got it! Duke was thinking of %d, and you found it in %s. Enter your name to post your score, or start a new game."
                .formatted(secretNumber, guesses(guessesMade));
    }

    /// Reveals the secret number after the player's last guess missed.
    public static String lost(final int guess, final GuessOutcome outcome, final int secretNumber) {
        return "%d is too %s, and that was your last guess. Duke was thinking of %d. Start a new game to try again."
                .formatted(guess, tooWhat(outcome), secretNumber);
    }

    /// Confirms a posted score by name.
    public static String scorePosted(final PlayerName playerName) {
        return "Nice one, %s! Your score is on the leaderboard.".formatted(playerName.value());
    }

    private static String tooWhat(final GuessOutcome outcome) {
        return outcome == TOO_LOW ? "low" : "high";
    }

    private static String whichWay(final GuessOutcome outcome) {
        return outcome == TOO_LOW ? "higher" : "lower";
    }

    private static String guesses(final int count) {
        return count == 1 ? "1 guess" : "%d guesses".formatted(count);
    }
}
