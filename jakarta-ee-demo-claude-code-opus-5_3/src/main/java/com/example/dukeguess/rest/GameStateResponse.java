package com.example.dukeguess.rest;

import com.example.dukeguess.game.GameStatus;
import com.example.dukeguess.game.GuessOutcome;
import com.example.dukeguess.game.GuessingGame;

import static com.example.dukeguess.game.GuessingGame.HIGHEST_NUMBER;
import static com.example.dukeguess.game.GuessingGame.LOWEST_NUMBER;
import static com.example.dukeguess.game.GuessingGame.MAXIMUM_GUESSES;
import static java.lang.String.format;

/// Everything the screen needs to draw the game after a request.
public record GameStateResponse(
        GameStatus status,
        GuessOutcome outcome,
        int guessesMade,
        int guessesRemaining,
        int maximumGuesses,
        int lowestNumber,
        int highestNumber,
        Integer secretNumber,
        String message) {

    public static GameStateResponse of(final GuessingGame game, final GuessOutcome outcome) {
        return new GameStateResponse(
                game.status(),
                outcome,
                game.guessesMade(),
                game.guessesRemaining(),
                MAXIMUM_GUESSES,
                LOWEST_NUMBER,
                HIGHEST_NUMBER,
                game.revealedSecretNumber().orElse(null),
                messageFor(game, outcome));
    }

    public static GameStateResponse of(final GuessingGame game) {
        return of(game, null);
    }

    private static String messageFor(final GuessingGame game, final GuessOutcome outcome) {
        return switch (game.status()) {
            case WON -> format("You got it in %s! Enter a name to post your score, or play again.",
                    guessWord(game.guessesMade()));
            case LOST -> {
                final var reveal = format("Out of guesses! Duke was thinking of %d. Play again?",
                        game.revealedSecretNumber().orElseThrow());
                yield outcome == null ? reveal : format("%s %s", outcome.message(), reveal);
            }
            case ACTIVE -> outcome == null
                    ? format("Duke is thinking of a number from %d to %d. Take a guess!",
                            LOWEST_NUMBER, HIGHEST_NUMBER)
                    : format("%s You have %s left.", outcome.message(), guessWord(game.guessesRemaining()));
        };
    }

    private static String guessWord(final int count) {
        return count == 1 ? "1 guess" : format("%d guesses", count);
    }
}
