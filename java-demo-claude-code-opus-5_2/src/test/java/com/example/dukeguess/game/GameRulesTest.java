package com.example.dukeguess.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GameRulesTest {

    @ParameterizedTest
    @CsvSource({"1,true", "5,true", "10,true", "0,false", "11,false", "-1,false"})
    void acceptsOnlyWholeNumbersFromOneToTen(final int guess, final boolean expected) {
        assertThat(GameRules.isGuessInRange(guess)).isEqualTo(expected);
    }

    @Test
    void namesTheRangeInTheMessageEveryRejectionShares() {
        assertThat(GameRules.guessRangeMessage()).contains("1").contains("10");
    }
}
