package tech.virtua.duke.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.virtua.duke.game.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.game.GameRules.LOWEST_NUMBER;

class RandomSecretNumberPickerTest {

    private static final int DRAWS = 10_000;

    /// Seeded so the suite cannot flake on an unlucky run.
    private final RandomGenerator generator = new Random(20_251_023L);

    private final SecretNumberPicker picker = new RandomSecretNumberPicker(() -> generator);

    @Test
    void onlyEverDrawsNumbersAPlayerCouldGuess() {
        assertTrue(draws().allMatch(GameRules::isGuessable));
    }

    @Test
    void drawsEveryNumberInRange() {
        final var drawn = new HashSet<Integer>();
        draws().forEach(drawn::add);
        assertEquals(HIGHEST_NUMBER - LOWEST_NUMBER + 1, drawn.size());
    }

    @Test
    void spreadsTheDrawsEvenlyEnoughToLookFair() {
        final var counts = new int[HIGHEST_NUMBER + 1];
        draws().forEach(number -> counts[number]++);
        final var expected = DRAWS / (HIGHEST_NUMBER - LOWEST_NUMBER + 1);
        final var tolerance = expected / 5;
        assertTrue(IntStream.rangeClosed(LOWEST_NUMBER, HIGHEST_NUMBER)
                .allMatch(number -> Math.abs(counts[number] - expected) <= tolerance));
    }

    private IntStream draws() {
        return IntStream.range(0, DRAWS).map(_ -> picker.pickSecretNumber());
    }
}
