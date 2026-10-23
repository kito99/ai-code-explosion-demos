package tech.virtua.duke.game;

import java.util.OptionalInt;
import java.util.regex.Pattern;

/// Turns whatever the player typed into a guess, or into nothing at all.
public final class GuessParser {

    /// Matches ASCII digits only, so numerals from other scripts are not guesses, and
    /// caps the length so nothing long enough to overflow an int reaches the parser.
    private static final Pattern WHOLE_NUMBER = Pattern.compile("[+-]?\\d{1,9}");

    private GuessParser() {
    }

    /// Reads a guess from raw player input, empty if it is not a whole number in range.
    public static OptionalInt parse(final String rawGuess) {
        if (rawGuess == null) {
            return OptionalInt.empty();
        }
        final var trimmed = rawGuess.trim();
        if (!WHOLE_NUMBER.matcher(trimmed).matches()) {
            return OptionalInt.empty();
        }
        final var guess = Integer.parseInt(trimmed);
        return GameRules.isGuessable(guess) ? OptionalInt.of(guess) : OptionalInt.empty();
    }
}
