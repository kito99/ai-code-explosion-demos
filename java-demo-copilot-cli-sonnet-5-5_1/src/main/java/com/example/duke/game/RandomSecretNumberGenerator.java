package com.example.duke.game;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

import static com.example.duke.game.GameRules.MAX_NUMBER;
import static com.example.duke.game.GameRules.MIN_NUMBER;

/// Draws each secret uniformly and independently from the allowed range.
@Component
public class RandomSecretNumberGenerator implements SecretNumberGenerator {

    @Override
    public int next() {
        return ThreadLocalRandom.current().nextInt(MIN_NUMBER, MAX_NUMBER + 1);
    }
}
