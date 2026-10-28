package com.example.guessduke.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.guessduke.leaderboard.LeaderboardRules.MAX_NAME_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerNameTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(PlayerName.parse("  Duke  ")).contains(new PlayerName("Duke"));
    }

    @Test
    void acceptsTheLongestAllowedName() {
        final var longest = "D".repeat(MAX_NAME_LENGTH);

        assertThat(PlayerName.parse(longest)).contains(new PlayerName(longest));
    }

    @Test
    void rejectsANameOneCharacterTooLong() {
        assertThat(PlayerName.parse("D".repeat(MAX_NAME_LENGTH + 1))).isEmpty();
    }

    @Test
    void countsLengthAfterTrimming() {
        final var padded = "   %s   ".formatted("D".repeat(MAX_NAME_LENGTH));

        assertThat(PlayerName.parse(padded)).isPresent();
    }

    @ParameterizedTest(name = "\"{0}\" leaves nothing visible")
    @ValueSource(strings = {"", "   ", "\t", "\n  \n"})
    @NullSource
    void rejectsNamesWithoutVisibleCharacters(final String rawName) {
        assertThat(PlayerName.parse(rawName)).isEmpty();
    }
}
