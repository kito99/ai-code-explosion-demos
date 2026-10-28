package com.example.guessduke.game;

import java.util.concurrent.atomic.AtomicInteger;

/// Hands out a secret number the test chose, so a round plays out predictably.
public class FixedSecretNumberGenerator implements SecretNumberGenerator {

    private final AtomicInteger secretNumber = new AtomicInteger(GameRules.LOWEST_NUMBER);

    /// Sets the number every round started from now on will hide.
    public void willChoose(final int nextSecretNumber) {
        secretNumber.set(nextSecretNumber);
    }

    @Override
    public int nextSecretNumber() {
        return secretNumber.get();
    }
}
