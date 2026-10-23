package tech.virtua.duke.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import tech.virtua.duke.game.GameSnapshot;
import tech.virtua.duke.game.GuessingGame;
import tech.virtua.duke.game.SecretNumberPicker;
import tech.virtua.duke.game.Submission;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/// The one game belonging to a browser session. Every change swaps the whole game in a
/// single compare-and-set, so two requests racing each other can never leave a guess
/// half applied.
@SessionScoped
public class PlayerGame implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final AtomicReference<GuessingGame> game = new AtomicReference<>();

    private SecretNumberPicker secretNumberPicker;

    /// Required by CDI, which subclasses session-scoped beans to proxy them.
    protected PlayerGame() {
    }

    @Inject
    public PlayerGame(final SecretNumberPicker secretNumberPicker) {
        this.secretNumberPicker = secretNumberPicker;
    }

    /// The game in play, dealing one if this session has not played yet.
    public GameSnapshot current() {
        return game.updateAndGet(current -> current == null ? deal() : current).snapshot();
    }

    /// Abandons whatever was in play and deals a fresh game.
    public GameSnapshot startNewGame() {
        final var dealt = deal();
        game.set(dealt);
        return dealt.snapshot();
    }

    /// Offers the game in play whatever the player typed, dealing one first if needed.
    public Submission submit(final String rawGuess) {
        while (true) {
            final var current = game.get();
            final var playedOn = current == null ? deal() : current;
            final var submission = playedOn.submit(rawGuess);
            if (game.compareAndSet(current, submission.game())) {
                return submission;
            }
        }
    }

    private GuessingGame deal() {
        return GuessingGame.dealtBy(secretNumberPicker);
    }
}
