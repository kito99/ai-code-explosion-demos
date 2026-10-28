package com.example.duke.game;

import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;

import static com.example.duke.game.GameRules.HIGHEST_NUMBER;
import static com.example.duke.game.GameRules.LOWEST_NUMBER;

/// Draws each secret number independently, with every value in range equally likely.
@Component
public class RandomSecretNumberGenerator implements SecretNumberGenerator {

    private final RandomGenerator randomGenerator = RandomGenerator.getDefault();

    @Override
    public int nextSecretNumber() {
        return randomGenerator.nextInt(LOWEST_NUMBER, HIGHEST_NUMBER + 1);
    }
}
