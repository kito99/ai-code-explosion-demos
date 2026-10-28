package com.example.duke.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.stream.IntStream;

import static com.example.duke.game.GameRules.HIGHEST_NUMBER;
import static com.example.duke.game.GameRules.LOWEST_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomSecretNumberGeneratorTest {

    /// Enough draws that every one of the ten values appearing is near-certain, while a draw
    /// outside the range would still be caught on the first offender.
    private static final int DRAWS = 2_000;

    private final SecretNumberGenerator generator = new RandomSecretNumberGenerator();

    @Test
    @DisplayName("every draw lands inside the allowed range")
    void drawsWithinRange() {
        IntStream.range(0, DRAWS).forEach(_ -> {
            final var drawn = generator.draw();
            assertTrue(GameRules.isWithinRange(drawn), () -> drawn + " is outside 1 to 10");
        });
    }

    @Test
    @DisplayName("every value in the range comes up")
    void coversTheWholeRange() {
        final var seen = new HashSet<Integer>();
        IntStream.range(0, DRAWS).forEach(_ -> seen.add(generator.draw()));

        assertEquals(HIGHEST_NUMBER - LOWEST_NUMBER + 1, seen.size(),
                () -> "expected every value from 1 to 10, saw " + seen);
    }
}
