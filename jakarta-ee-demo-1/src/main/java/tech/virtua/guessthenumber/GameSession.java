package tech.virtua.guessthenumber;

import jakarta.enterprise.context.SessionScoped;
import tech.virtua.guessthenumber.game.Acceptance;
import tech.virtua.guessthenumber.game.GameSnapshot;
import tech.virtua.guessthenumber.game.GuessingGame;
import tech.virtua.guessthenumber.game.SubmitResult;

import java.io.Serial;
import java.io.Serializable;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The one game belonging to a browser session. Every method is synchronized and
 * hands back an immutable snapshot, so a double-clicked Guess button cannot leave
 * two requests interleaving over the same game.
 */
@SessionScoped
public class GameSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private GuessingGame game;

    /** The game in play, starting one on first use so a fresh page load can just read state. */
    public synchronized GameSnapshot current() {
        if (game == null) {
            game = newGame();
        }
        return game.snapshot();
    }

    /** Abandons whatever game is in play and deals a fresh secret number. */
    public synchronized GameSnapshot startNewGame() {
        game = newGame();
        return game.snapshot();
    }

    /**
     * Submits raw player input as a guess. Input that is not a whole number from 1 to
     * 10 is rejected without costing a guess, and a finished game takes no more guesses.
     */
    public synchronized SubmitResult submit(String rawInput) {
        if (game == null) {
            game = newGame();
        }
        if (game.isOver()) {
            return new SubmitResult(Acceptance.GAME_OVER, game.snapshot());
        }

        OptionalInt guess = GuessingGame.parseGuess(rawInput);
        if (guess.isEmpty()) {
            return new SubmitResult(Acceptance.INVALID_INPUT, game.snapshot());
        }

        game.guess(guess.getAsInt());
        return new SubmitResult(Acceptance.ACCEPTED, game.snapshot());
    }

    private static GuessingGame newGame() {
        return GuessingGame.withRandomSecret(ThreadLocalRandom.current());
    }
}
