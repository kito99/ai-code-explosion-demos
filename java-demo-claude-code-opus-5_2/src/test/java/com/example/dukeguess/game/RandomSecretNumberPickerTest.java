package com.example.dukeguess.game;

import java.util.HashSet;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static com.example.dukeguess.game.GameRules.HIGHEST_NUMBER;
import static com.example.dukeguess.game.GameRules.LOWEST_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;

class RandomSecretNumberPickerTest {

    private final RandomSecretNumberPicker picker = new RandomSecretNumberPicker();

    @RepeatedTest(50)
    void picksWithinThePlayableRange() {
        assertThat(picker.pick()).isBetween(LOWEST_NUMBER, HIGHEST_NUMBER);
    }

    @Test
    void canDrawEveryNumberInTheRange() {
        final var drawn = new HashSet<Integer>();

        for (var draw = 0; draw < 1_000; draw++) {
            drawn.add(picker.pick());
        }

        assertThat(drawn).hasSize(HIGHEST_NUMBER - LOWEST_NUMBER + 1);
    }

    @Test
    void asksTheGeneratorForTheWholeRangeInclusive() {
        final var highestOnly = new RandomSecretNumberPicker(fixedToUpperBound());

        assertThat(highestOnly.pick()).isEqualTo(HIGHEST_NUMBER);
    }

    /// A generator that always returns the top of the range it is given, which catches an
    /// exclusive upper bound that would leave 10 undrawable.
    private static RandomGenerator fixedToUpperBound() {
        return new RandomGenerator() {

            @Override
            public long nextLong() {
                return 0L;
            }

            @Override
            public int nextInt(final int origin, final int bound) {
                return bound - 1;
            }
        };
    }
}
