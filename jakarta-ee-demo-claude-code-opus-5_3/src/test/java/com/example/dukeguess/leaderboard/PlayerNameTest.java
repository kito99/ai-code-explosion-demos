package com.example.dukeguess.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.dukeguess.leaderboard.PlayerName.MAXIMUM_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PlayerNameTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(PlayerName.of("  Duke  ").value()).isEqualTo("Duke");
    }

    @Test
    void acceptsANameOfExactlyTheMaximumLength() {
        final var longestName = "D".repeat(MAXIMUM_LENGTH);

        assertThat(PlayerName.of(longestName).value()).isEqualTo(longestName);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n", "\u200B", "\u200B \u200B"})
    void rejectsNamesWithoutAVisibleCharacter(final String blankName) {
        assertThatExceptionOfType(InvalidPlayerNameException.class)
                .isThrownBy(() -> PlayerName.of(blankName));
    }

    @Test
    void rejectsNamesLongerThanTheMaximumAfterTrimming() {
        final var tooLongName = " %s ".formatted("D".repeat(MAXIMUM_LENGTH + 1));

        assertThatExceptionOfType(InvalidPlayerNameException.class)
                .isThrownBy(() -> PlayerName.of(tooLongName));
    }
}
