package com.example.dukeguess.leaderboard;

import java.util.Objects;

/// The name a winner types so their result can be posted.
///
/// This is a label on a leaderboard row, not an account: nothing identifies the player
/// behind it, and two players may use the same name.
public record PlayerName(String value) {

    public static final int MAX_LENGTH = 30;

    public PlayerName {
        Objects.requireNonNull(value, "value");
    }

    /// Trims surrounding whitespace and accepts the result when it has at least one
    /// visible character and is no longer than [#MAX_LENGTH] characters.
    ///
    /// Throws [InvalidPlayerNameException] when it does not.
    public static PlayerName of(final String submitted) {
        final var trimmed = submitted == null ? "" : submitted.strip();
        if (!hasVisibleCharacter(trimmed) || lengthInCharacters(trimmed) > MAX_LENGTH) {
            throw new InvalidPlayerNameException();
        }
        return new PlayerName(trimmed);
    }

    /// A name built only from spaces, control codes, or invisible formatting characters
    /// such as a zero-width space would show as a blank leaderboard row, so none of those
    /// count as visible.
    private static boolean hasVisibleCharacter(final String candidate) {
        return candidate.codePoints().anyMatch(PlayerName::isVisible);
    }

    private static boolean isVisible(final int codePoint) {
        return !Character.isWhitespace(codePoint)
                && !Character.isSpaceChar(codePoint)
                && !Character.isISOControl(codePoint)
                && Character.getType(codePoint) != Character.FORMAT;
    }

    /// Counts characters the way a reader would, so a name of emoji is measured by the
    /// characters on screen rather than by how many code units they take up.
    private static int lengthInCharacters(final String candidate) {
        return candidate.codePointCount(0, candidate.length());
    }

    @Override
    public String toString() {
        return value;
    }
}
