package com.example.guessduke.game;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import static com.example.guessduke.game.GameRules.HIGHEST_NUMBER;
import static com.example.guessduke.game.GameRules.LOWEST_NUMBER;

/// Draws secret numbers uniformly across the playable range, independently on every call.
@Component
public class RandomSecretNumberGenerator implements SecretNumberGenerator {

    @Override
    public int nextSecretNumber() {
        return ThreadLocalRandom.current().nextInt(LOWEST_NUMBER, HIGHEST_NUMBER + 1);
    }
}
