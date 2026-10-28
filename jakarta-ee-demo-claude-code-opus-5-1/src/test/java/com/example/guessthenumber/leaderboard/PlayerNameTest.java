package com.example.guessthenumber.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerNameTest {

    @Test
    void keepsANameThatAlreadyFits() {
        assertEquals("Ada", new PlayerName("Ada").value());
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals("Ada", new PlayerName("  Ada\t\n").value());
    }

    @Test
    void acceptsTheLongestAllowedName() {
        final var longest = "D".repeat(PlayerName.MAX_LENGTH);

        assertEquals(longest, new PlayerName(longest).value());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n   "})
    void refusesANameWithoutAVisibleCharacter(final String blank) {
        assertThrows(InvalidPlayerNameException.class, () -> new PlayerName(blank));
    }

    @Test
    void refusesANameLongerThanTheLimit() {
        final var tooLong = "D".repeat(PlayerName.MAX_LENGTH + 1);

        assertThrows(InvalidPlayerNameException.class, () -> new PlayerName(tooLong));
    }

    @Test
    void measuresLengthAfterTrimming() {
        final var paddedButShortEnough = "  %s  ".formatted("D".repeat(PlayerName.MAX_LENGTH));

        assertEquals(PlayerName.MAX_LENGTH, new PlayerName(paddedButShortEnough).value().length());
    }
}
