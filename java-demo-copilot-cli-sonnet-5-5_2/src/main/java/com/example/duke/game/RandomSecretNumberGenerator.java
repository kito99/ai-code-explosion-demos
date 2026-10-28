package com.example.duke.game;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

@Component
public class RandomSecretNumberGenerator implements SecretNumberGenerator {

    private final RandomGenerator randomGenerator = RandomGenerator.getDefault();

    @Override
    public int nextSecret() {
        return randomGenerator.nextInt(GameRules.MIN_NUMBER, GameRules.MAX_NUMBER + 1);
    }
}
