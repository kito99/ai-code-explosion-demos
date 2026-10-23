package tech.virtua.duke;

import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/// One player's game: the secret, the guesses spent on it, and how it ended.
/// Concurrent requests on the same game always see a whole state, never a
/// half-applied guess.
public final class GuessingGame {

    /// Digits only, length-bounded so parsing can never overflow.
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,3}");

    private final SecretNumberPicker secretNumberPicker;
    private final AtomicReference<GameState> state;

    public GuessingGame(final SecretNumberPicker secretNumberPicker) {
        this.secretNumberPicker = requireNonNull(secretNumberPicker);
        this.state = new AtomicReference<>(freshState());
    }

    /// The current state of this game.
    public GameState state() {
        return state.get();
    }

    /// Throws out the current game and starts a new one with a new secret.
    public GameState restart() {
        final var fresh = freshState();
        state.set(fresh);
        return fresh;
    }

    /// Applies `rawGuess` as typed by the player and returns the resulting
    /// state. Anything that is not a whole number in range is rejected without
    /// counting against the guess limit.
    public GameState submitGuess(final String rawGuess) {
        final var guess = parseGuess(rawGuess);
        if (guess.isEmpty()) {
            return state.updateAndGet(GameState::afterRejection);
        }
        final var accepted = guess.getAsInt();
        return state.updateAndGet(current -> current.afterGuess(accepted));
    }

    private GameState freshState() {
        return GameState.startWith(secretNumberPicker.pickSecretNumber());
    }

    private static OptionalInt parseGuess(final String rawGuess) {
        final var trimmed = requireNonNull(rawGuess).strip();
        if (!WHOLE_NUMBER.matcher(trimmed).matches()) {
            return OptionalInt.empty();
        }
        final var value = Integer.parseInt(trimmed);
        return GameRules.isInRange(value) ? OptionalInt.of(value) : OptionalInt.empty();
    }
}
