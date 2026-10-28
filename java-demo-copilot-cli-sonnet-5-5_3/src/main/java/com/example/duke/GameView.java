package com.example.duke;

import static com.example.duke.GameRules.MAX_GUESSES;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Optional;

/** JSON representation of a game after an action, with a plain-language message. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameView(
        String gameId,
        GameStatus status,
        GuessResult result,
        int guessesMade,
        int guessesRemaining,
        int maxGuesses,
        Integer secretNumber,
        String message) {

    public static GameView of(final Game game, final Optional<GuessResult> result) {
        final var status = game.status();
        return new GameView(
                game.id(),
                status,
                result.orElse(null),
                game.guessesMade(),
                game.guessesRemaining(),
                MAX_GUESSES,
                game.revealedSecret().orElse(null),
                message(game, status, result));
    }

    private static String message(final Game game, final GameStatus status, final Optional<GuessResult> result) {
        return switch (status) {
            case WON -> String.format(
                    "You got it! Duke was thinking of that number. It took you %d %s. "
                            + "Enter your name to post your score, or start a new game.",
                    game.guessesMade(), game.guessesMade() == 1 ? "guess" : "guesses");
            case LOST -> String.format(
                    "Out of guesses! Duke was thinking of %d. Start a new game to try again.",
                    game.revealedSecret().orElseThrow());
            case IN_PROGRESS -> result.map(guessResult -> inProgressMessage(game, guessResult))
                    .orElse(String.format(
                            "Duke is thinking of a number from %d to %d. You have %d guesses. Type your guess and press Enter.",
                            GameRules.MIN_NUMBER, GameRules.MAX_NUMBER, MAX_GUESSES));
        };
    }

    private static String inProgressMessage(final Game game, final GuessResult result) {
        final var direction = result == GuessResult.TOO_HIGH ? "too high" : "too low";
        return String.format("%s is %s. Try again; you have %d %s left.",
                "That guess", direction, game.guessesRemaining(),
                game.guessesRemaining() == 1 ? "guess" : "guesses");
    }
}
