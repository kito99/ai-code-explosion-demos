package com.example.duke.leaderboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.duke.leaderboard.PlayerName.MAX_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerNameTest {

    @Test
    @DisplayName("surrounding whitespace is trimmed off")
    void trimsSurroundingWhitespace() {
        assertEquals("Duke", new PlayerName("  Duke \t\n").value());
    }

    @Test
    @DisplayName("a name of exactly the maximum length is accepted")
    void acceptsTheLongestAllowedName() {
        final var longest = "D".repeat(MAX_LENGTH);

        assertTrue(PlayerName.isAcceptable(longest));
        assertEquals(longest, new PlayerName(longest).value());
    }

    @Test
    @DisplayName("length is measured after trimming")
    void measuresLengthAfterTrimming() {
        final var paddedButShort = "   " + "D".repeat(MAX_LENGTH) + "   ";

        assertTrue(PlayerName.isAcceptable(paddedButShort));
        assertEquals(MAX_LENGTH, new PlayerName(paddedButShort).value().length());
    }

    @ParameterizedTest(name = "[{0}] is not an acceptable name")
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n  "})
    @DisplayName("a name needs at least one visible character")
    void rejectsNamesWithNothingVisible(final String blank) {
        assertFalse(PlayerName.isAcceptable(blank));
    }

    @Test
    @DisplayName("a name longer than the maximum is rejected")
    void rejectsTooLongNames() {
        final var tooLong = "D".repeat(MAX_LENGTH + 1);

        assertFalse(PlayerName.isAcceptable(tooLong));
        assertThrows(InvalidPlayerNameException.class, () -> new PlayerName(tooLong));
    }

    @Test
    @DisplayName("an unacceptable name cannot be constructed")
    void refusesToWrapAnUnacceptableName() {
        assertThrows(InvalidPlayerNameException.class, () -> new PlayerName("   "));
        assertThrows(NullPointerException.class, () -> new PlayerName(null));
    }

    @ParameterizedTest(name = "a name of only [{0}] has nothing to show")
    @ValueSource(strings = {"\u200B", "\u200B\u200C\u200D", "\uFEFF", "\u0001"})
    @DisplayName("characters that draw nothing do not count as visible")
    void rejectsNamesWithNothingToShow(final String invisible) {
        assertFalse(PlayerName.isAcceptable(invisible));
        assertThrows(InvalidPlayerNameException.class, () -> new PlayerName(invisible));
    }

    @Test
    @DisplayName("a name carrying a control character is turned away rather than failing to store")
    void rejectsControlCharacters() {
        assertFalse(PlayerName.isAcceptable("Duke\u0000Fan"),
                "PostgreSQL cannot hold NUL in a text column");
        assertFalse(PlayerName.isAcceptable("Duke\u0007"));
        assertFalse(PlayerName.isAcceptable("Du\u200Bke"), "a zero-width mark shows as nothing");
    }

    @Test
    @DisplayName("ordinary names with punctuation and accents are still fine")
    void acceptsEverydayNames() {
        assertTrue(PlayerName.isAcceptable("Duke"));
        assertTrue(PlayerName.isAcceptable("Ana-María O'Brien"));
        assertTrue(PlayerName.isAcceptable("デューク"));
        assertTrue(PlayerName.isAcceptable("Duke \uD83D\uDC4B"));
    }

    @Test
    @DisplayName("two players may use the same name")
    void allowsDuplicateNames() {
        assertEquals(new PlayerName("Duke"), new PlayerName("Duke"));
    }
}
