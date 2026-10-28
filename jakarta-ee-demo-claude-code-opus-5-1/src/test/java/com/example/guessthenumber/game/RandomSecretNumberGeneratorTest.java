package com.example.guessthenumber.game;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomSecretNumberGeneratorTest {

    private static final int DRAWS = 10_000;
    private static final int NUMBERS_IN_RANGE = 10;

    private final SecretNumberGenerator generator = new RandomSecretNumberGenerator();

    @Test
    void drawsEveryNumberInTheRangeAndNothingElse() {
        final var drawn = IntStream.range(0, DRAWS)
                .map(draw -> generator.nextSecretNumber(GameRules.STANDARD))
                .distinct()
                .sorted()
                .boxed()
                .toList();

        assertEquals(NUMBERS_IN_RANGE, drawn.size());
        assertTrue(drawn.stream().allMatch(GameRules.STANDARD::allows));
    }

    @Test
    void handlesARangeOfOne() {
        final var single = new GameRules(4, 4, 1);

        assertEquals(4, generator.nextSecretNumber(single));
    }
}
