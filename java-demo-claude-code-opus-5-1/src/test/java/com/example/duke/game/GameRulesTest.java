package com.example.duke.game;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.example.duke.game.GameRules.isWithinRange;
import static org.assertj.core.api.Assertions.assertThat;

class GameRulesTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10})
    void acceptsNumbersDukeCouldPick(final int number) {
        assertThat(isWithinRange(number)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 11, 100})
    void rejectsNumbersOutsideTheRange(final int number) {
        assertThat(isWithinRange(number)).isFalse();
    }
}
