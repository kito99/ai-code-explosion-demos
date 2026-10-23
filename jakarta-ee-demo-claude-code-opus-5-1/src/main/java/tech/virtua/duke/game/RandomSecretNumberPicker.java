package tech.virtua.duke.game;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import static tech.virtua.duke.game.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.game.GameRules.LOWEST_NUMBER;

/// Draws each number in range with equal probability, independently of earlier draws.
public final class RandomSecretNumberPicker implements SecretNumberPicker {

    private final Supplier<RandomGenerator> randomGenerators;

    /// One generator per thread, because a single picker is shared by every player.
    public RandomSecretNumberPicker() {
        this(ThreadLocalRandom::current);
    }

    public RandomSecretNumberPicker(final Supplier<RandomGenerator> randomGenerators) {
        this.randomGenerators = Objects.requireNonNull(randomGenerators);
    }

    @Override
    public int pickSecretNumber() {
        return randomGenerators.get().nextInt(LOWEST_NUMBER, HIGHEST_NUMBER + 1);
    }
}
