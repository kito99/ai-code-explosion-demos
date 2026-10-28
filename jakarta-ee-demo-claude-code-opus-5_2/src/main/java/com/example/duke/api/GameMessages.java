package com.example.duke.api;

import com.example.duke.game.GameSnapshot;

import static com.example.duke.game.GameRules.HIGHEST_NUMBER;
import static com.example.duke.game.GameRules.LOWEST_NUMBER;
import static com.example.duke.game.GameRules.MAX_GUESSES;
import static java.lang.String.format;

/// The player-facing wording for every state the game can be in. Each message says what just
/// happened and what the player can do next, and the server is the only place it is written,
/// so the API and the screen always agree.
public final class GameMessages {

    private GameMessages() {
    }

    /// The message for the round as it currently stands.
    public static String forGame(final GameSnapshot game) {
        return switch (game.status()) {
            case IN_PROGRESS -> game.lastOutcome()
                    .map(outcome -> switch (outcome) {
                        case TOO_LOW -> format("Too low. Try a higher number — %s.", guessesLeft(game));
                        case TOO_HIGH -> format("Too high. Try a lower number — %s.", guessesLeft(game));
                        case CORRECT -> won(game.guessesMade());
                    })
                    .orElseGet(GameMessages::started);
            case WON -> won(game.guessesMade());
            case LOST -> lost(game.revealedSecretNumber().orElseThrow());
        };
    }

    public static String started() {
        return format("Duke is thinking of a number from %d to %d. You have %d guesses — good luck!",
                LOWEST_NUMBER, HIGHEST_NUMBER, MAX_GUESSES);
    }

    public static String won(final int guessesTaken) {
        return format("You got it in %s! Enter a name to post your score, or play again.",
                guessCount(guessesTaken));
    }

    public static String lost(final int secretNumber) {
        return format("Out of guesses. Duke was thinking of %d. Play again to try your luck.",
                secretNumber);
    }

    public static String scorePosted(final String playerName, final int guessesTaken) {
        return format("Thanks, %s! Your win in %s is on the leaderboard. Play again, or go see where you landed.",
                playerName, guessCount(guessesTaken));
    }

    private static String guessesLeft(final GameSnapshot game) {
        return format("%s left", guessCount(game.guessesRemaining()));
    }

    private static String guessCount(final int count) {
        return format("%d %s", count, count == 1 ? "guess" : "guesses");
    }
}
