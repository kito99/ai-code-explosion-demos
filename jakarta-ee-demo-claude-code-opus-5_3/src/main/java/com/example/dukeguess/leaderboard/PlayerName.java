package com.example.dukeguess.leaderboard;

import java.util.function.IntPredicate;

import static java.lang.String.format;

/// A leaderboard name that has been trimmed and checked. Not an account: two
/// players may post under the same name.
public record PlayerName(String value) {

    public static final int MAXIMUM_LENGTH = 30;

    /// Zero-width and formatting characters survive trimming but would leave a blank row.
    private static final IntPredicate VISIBLE = codePoint ->
            !Character.isWhitespace(codePoint)
                    && !Character.isSpaceChar(codePoint)
                    && Character.getType(codePoint) != Character.FORMAT
                    && Character.getType(codePoint) != Character.CONTROL;

    /// Trims `rawName` and accepts it when it still has a visible character and
    /// fits within [#MAXIMUM_LENGTH].
    public static PlayerName of(final String rawName) {
        final var trimmedName = rawName == null ? "" : rawName.strip();
        if (trimmedName.codePoints().noneMatch(VISIBLE)) {
            throw new InvalidPlayerNameException("Please enter a name so we can post your score.");
        }
        if (trimmedName.length() > MAXIMUM_LENGTH) {
            throw new InvalidPlayerNameException(
                    format("That name is a bit long. Please keep it to %d characters or fewer.", MAXIMUM_LENGTH));
        }
        return new PlayerName(trimmedName);
    }
}
