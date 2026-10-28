package com.example.dukeguess.game;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import static com.example.dukeguess.game.GameRules.HIGHEST_NUMBER;
import static com.example.dukeguess.game.GameRules.LOWEST_NUMBER;

/// Draws each secret number independently and uniformly from the playable range.
@Component
public class RandomSecretNumberPicker implements SecretNumberPicker {

    private final RandomGenerator randomGenerator;

    public RandomSecretNumberPicker() {
        this(RandomGenerator.getDefault());
    }

    RandomSecretNumberPicker(final RandomGenerator randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    @Override
    public int pick() {
        return randomGenerator.nextInt(LOWEST_NUMBER, HIGHEST_NUMBER + 1);
    }
}
