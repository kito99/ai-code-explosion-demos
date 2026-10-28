package com.example.duke.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LeaderboardServiceTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t\n", "1234567890123456789012345678901",
            "  1234567890123456789012345678901  "})
    void rejectsBlankOrTooLongNames(final String name) {
        assertThatThrownBy(() -> LeaderboardService.validatedName(name)).isInstanceOf(InvalidNameException.class);
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(LeaderboardService.validatedName("  Duke  ")).isEqualTo("Duke");
    }

    @Test
    void acceptsNamesOfExactlyThirtyCharacters() {
        final var name = "a".repeat(LeaderboardService.MAX_NAME_LENGTH);

        assertThat(LeaderboardService.validatedName(name)).isEqualTo(name);
    }

    @Test
    void countsCharactersNotUtf16Units() {
        final var name = "🎉".repeat(LeaderboardService.MAX_NAME_LENGTH);

        assertThat(LeaderboardService.validatedName(name)).isEqualTo(name);
    }
}
