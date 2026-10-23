package tech.virtua.duke;

import static tech.virtua.duke.GameRules.GUESS_LIMIT;
import static tech.virtua.duke.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.GameRules.LOWEST_NUMBER;

/// Renders a [GameState] for the browser. The rules travel with the state so
/// the front end never hard-codes them, and the secret number is only present
/// once the game is over.
public final class GameStateJson {

    private GameStateJson() {
    }

    public static String render(final GameState state) {
        return JsonWriter.object()
                .put("status", state.status())
                .put("feedback", state.feedback())
                .put("message", GameMessages.describe(state))
                .put("guessesMade", state.guessesMade())
                .put("guessesRemaining", state.guessesRemaining())
                .put("guessLimit", GUESS_LIMIT)
                .put("lowestNumber", LOWEST_NUMBER)
                .put("highestNumber", HIGHEST_NUMBER)
                .putOrNull("lastGuess", state.lastGuess())
                .putOrNull("secretNumber", state.revealedSecretNumber())
                .build();
    }
}
