package com.example.duke.leaderboard;

import com.example.duke.game.GameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerNameTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(new PlayerName("  Duke \t").value()).isEqualTo("Duke");
    }

    @Test
    void acceptsExactlyThirtyCharacters() {
        assertThat(new PlayerName("a".repeat(30)).value()).hasSize(30);
    }

    @Test
    void rejectsThirtyOneCharacters() {
        assertThatThrownBy(() -> new PlayerName("a".repeat(31))).isInstanceOf(GameException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejectsBlankNames(final String name) {
        assertThatThrownBy(() -> new PlayerName(name)).isInstanceOf(GameException.class);
    }
}
