package com.example.duke.game;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ThreadLocalRandom;

import static com.example.duke.game.GameRules.HIGHEST_NUMBER;
import static com.example.duke.game.GameRules.LOWEST_NUMBER;

/// Draws the secret number uniformly at random. Every value in the range is equally likely
/// and each draw is independent of the last, so the same number can come up twice in a row.
@ApplicationScoped
public class RandomSecretNumberGenerator implements SecretNumberGenerator {

    @Override
    public int draw() {
        return ThreadLocalRandom.current().nextInt(LOWEST_NUMBER, HIGHEST_NUMBER + 1);
    }
}
