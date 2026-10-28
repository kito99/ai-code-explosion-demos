package com.example.dukeguess.game;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.random.RandomGenerator;

/// Draws each secret number independently and uniformly.
@ApplicationScoped
public class RandomSecretNumberGenerator implements SecretNumberGenerator {

    private final RandomGenerator randomGenerator = RandomGenerator.getDefault();

    @Override
    public int nextSecretNumber(final int lowestNumber, final int highestNumber) {
        return randomGenerator.nextInt(lowestNumber, highestNumber + 1);
    }
}
