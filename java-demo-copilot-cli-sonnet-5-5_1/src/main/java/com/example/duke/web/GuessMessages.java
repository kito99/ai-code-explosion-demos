package com.example.duke.web;

import com.example.duke.game.GuessOutcome;
import com.example.duke.game.GuessResult;

final class GuessMessages {

    private GuessMessages() {
    }

    static String describe(final GuessResult result) {
        final var game = result.game();
        return switch (result.outcome()) {
            case CORRECT -> "You got it in %d %s! Post your score to the leaderboard or play again."
                    .formatted(game.guessesMade(), game.guessesMade() == 1 ? "guess" : "guesses");
            case TOO_HIGH, TOO_LOW -> {
                final var direction = result.outcome() == GuessOutcome.TOO_HIGH ? "high" : "low";
                yield switch (game.status()) {
                    case LOST -> "Too %s, and you're out of guesses. Duke was thinking of %d. Play again?"
                            .formatted(direction, game.revealedSecret().orElseThrow());
                    default -> "Too %s. You have %d %s left. Try again!"
                            .formatted(direction, game.guessesRemaining(), game.guessesRemaining() == 1 ? "guess" : "guesses");
                };
            }
        };
    }
}
