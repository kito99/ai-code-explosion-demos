package tech.virtua.duke.web;

import jakarta.json.bind.annotation.JsonbNillable;
import tech.virtua.duke.game.GameSnapshot;
import tech.virtua.duke.game.GameStatus;
import tech.virtua.duke.game.GuessFeedback;
import tech.virtua.duke.game.SubmissionOutcome;

import java.util.OptionalInt;

import static tech.virtua.duke.game.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.game.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.game.GameRules.LOWEST_NUMBER;

/// The JSON every endpoint answers with, so the page renders any answer the same way.
/// `lastGuess` and `secretNumber` are written as null rather than left out, which keeps
/// the shape fixed for the page and for anyone reading the API.
@JsonbNillable
public record GameView(GameStatus status,
                       SubmissionOutcome outcome,
                       GuessFeedback feedback,
                       String message,
                       int guessesMade,
                       int guessesRemaining,
                       int guessLimit,
                       int lowestNumber,
                       int highestNumber,
                       Integer lastGuess,
                       Integer secretNumber) {

    /// Renders a game, and what the submission that led to it did.
    public static GameView of(final GameSnapshot game, final SubmissionOutcome outcome) {
        return new GameView(game.status(),
                outcome,
                game.feedback(),
                GameMessages.describe(game, outcome),
                game.guessesMade(),
                game.guessesRemaining(),
                GUESS_LIMIT,
                LOWEST_NUMBER,
                HIGHEST_NUMBER,
                boxed(game.lastGuess()),
                boxed(game.secretNumber()));
    }

    private static Integer boxed(final OptionalInt number) {
        return number.isPresent() ? number.getAsInt() : null;
    }
}
