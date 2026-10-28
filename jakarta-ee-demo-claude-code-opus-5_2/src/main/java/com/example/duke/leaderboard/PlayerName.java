package com.example.duke.leaderboard;

import static java.util.Objects.requireNonNull;

/// The name a winner types so their score can be posted. It is not an account: two players
/// can use the same name, and nothing is stored about who they are.
///
/// A name is accepted when, once surrounding whitespace is trimmed, it still has a visible
/// character and is no longer than [#MAX_LENGTH] characters. Nothing is judged about what
/// the name says — only that there is something there to show.
public record PlayerName(String value) {

    public static final int MAX_LENGTH = 30;

    /// The name rules in plain words, used everywhere they are enforced so a player always
    /// reads the same explanation.
    public static final String RULE_MESSAGE =
            "Please enter a name with at least one character and no more than 30.";

    public PlayerName {
        requireNonNull(value, "value");
        if (!isAcceptable(value)) {
            throw new InvalidPlayerNameException();
        }
        value = value.strip();
    }

    /// Whether `candidate` passes the name rules. Null is never acceptable.
    public static boolean isAcceptable(final String candidate) {
        if (candidate == null) {
            return false;
        }
        final var trimmed = candidate.strip();
        return trimmed.length() <= MAX_LENGTH
                && trimmed.codePoints().anyMatch(PlayerName::isVisible)
                && trimmed.codePoints().noneMatch(PlayerName::isUnprintable);
    }

    /// Whether a code point puts ink on the leaderboard. Zero-width and formatting marks do
    /// not, so a name made only of those has nothing to show and is not a name.
    private static boolean isVisible(final int codePoint) {
        return !Character.isWhitespace(codePoint)
                && !Character.isSpaceChar(codePoint)
                && !isUnprintable(codePoint);
    }

    /// Characters that draw nothing: control characters such as NUL, which PostgreSQL will
    /// not store in a text column at all, and the zero-width formatting marks.
    private static boolean isUnprintable(final int codePoint) {
        return switch (Character.getType(codePoint)) {
            case Character.CONTROL, Character.FORMAT -> true;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
