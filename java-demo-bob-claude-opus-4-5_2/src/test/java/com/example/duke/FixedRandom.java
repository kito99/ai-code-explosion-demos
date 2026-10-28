package com.example.duke;

import java.util.Random;

/**
 * Test helper providing a deterministic {@link Random} stub.
 * {@code nextInt(bound)} always returns {@code fixedValue % bound}, so
 * {@code new FixedRandom(4)} with {@code nextInt(10) + 1} always yields 5.
 */
public final class FixedRandom extends Random {

    private final int fixedValue;

    public FixedRandom(int fixedValue) {
        super();
        this.fixedValue = fixedValue;
    }

    @Override
    public int nextInt(int bound) {
        return fixedValue % bound;
    }
}
