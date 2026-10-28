package com.example.guessthenumber.game;

import jakarta.inject.Singleton;

import java.util.concurrent.ThreadLocalRandom;

/// Draws each secret number independently and uniformly from the allowed range.
@Singleton
public class RandomSecretNumberGenerator implements SecretNumberGenerator {

    @Override
    public int nextSecretNumber(final GameRules rules) {
        return ThreadLocalRandom.current().nextInt(rules.lowestNumber(), rules.highestNumber() + 1);
    }
}
