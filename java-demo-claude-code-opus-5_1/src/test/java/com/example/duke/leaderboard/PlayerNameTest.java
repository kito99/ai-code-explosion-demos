package com.example.duke.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.duke.leaderboard.PlayerName.MAX_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerNameTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(PlayerName.of("  Duke Fan \n").value()).isEqualTo("Duke Fan");
    }

    @Test
    void acceptsTheLongestAllowedName() {
        final var longest = "D".repeat(MAX_LENGTH);

        assertThat(PlayerName.of(longest).value()).isEqualTo(longest);
    }

    @Test
    void rejectsNamesLongerThanTheLimit() {
        assertThatThrownBy(() -> PlayerName.of("D".repeat(MAX_LENGTH + 1)))
                .isInstanceOf(InvalidPlayerNameException.class)
                .hasMessageContaining(String.valueOf(MAX_LENGTH));
    }

    @Test
    void countsCharactersTheWayAPlayerDoes() {
        final var thirtyEmoji = "\uD83D\uDE00".repeat(MAX_LENGTH);

        assertThat(PlayerName.of(thirtyEmoji).value()).isEqualTo(thirtyEmoji);
        assertThatThrownBy(() -> PlayerName.of("\uD83D\uDE00".repeat(MAX_LENGTH + 1)))
                .isInstanceOf(InvalidPlayerNameException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    void rejectsNamesWithoutAVisibleCharacter(final String rawName) {
        assertThatThrownBy(() -> PlayerName.of(rawName)).isInstanceOf(InvalidPlayerNameException.class);
    }
}
