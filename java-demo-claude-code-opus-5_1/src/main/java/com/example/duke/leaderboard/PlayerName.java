package com.example.duke.leaderboard;

import java.util.Objects;

/// A name a winner types so their result can be posted. It is trimmed of surrounding whitespace,
/// must keep at least one visible character, and must fit [#MAX_LENGTH] characters.
///
/// Length is counted in code points, so a name of emoji is as long as it looks to the player.
public record PlayerName(String value) {

    /// Longest name the leaderboard accepts.
    public static final int MAX_LENGTH = 30;

    public PlayerName {
        Objects.requireNonNull(value, "value is required");
    }

    /// Trims and validates `rawName`.
    ///
    /// @throws InvalidPlayerNameException when the name is blank, invisible, or too long
    public static PlayerName of(final String rawName) {
        if (rawName == null) {
            throw new InvalidPlayerNameException("Please enter a name so your score can be posted.");
        }
        final var trimmed = rawName.strip();
        if (!hasVisibleCharacter(trimmed)) {
            throw new InvalidPlayerNameException("Please enter a name with at least one visible character.");
        }
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new InvalidPlayerNameException(
                    "Please use a name of %d characters or fewer.".formatted(MAX_LENGTH));
        }
        return new PlayerName(trimmed);
    }

    private static boolean hasVisibleCharacter(final String candidate) {
        return candidate.codePoints()
                .anyMatch(codePoint -> !Character.isWhitespace(codePoint) && !Character.isISOControl(codePoint));
    }
}
