package com.example.dukeguess.game;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static com.example.dukeguess.game.GuessingGame.HIGHEST_NUMBER;
import static com.example.dukeguess.game.GuessingGame.LOWEST_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;

class RandomSecretNumberGeneratorTest {

    private static final int DRAWS = 2_000;

    private final RandomSecretNumberGenerator generator = new RandomSecretNumberGenerator();

    @Test
    void drawsEveryNumberInTheRangeAndNothingOutsideIt() {
        final var draws = IntStream.range(0, DRAWS)
                .map(_ -> generator.nextSecretNumber(LOWEST_NUMBER, HIGHEST_NUMBER))
                .boxed()
                .distinct()
                .sorted()
                .toList();

        assertThat(draws).containsExactlyElementsOf(
                IntStream.rangeClosed(LOWEST_NUMBER, HIGHEST_NUMBER).boxed().toList());
    }
}
