package com.example.guessthenumber.game;

/// Draws the secret number for a new game.
@FunctionalInterface
public interface SecretNumberGenerator {

    /// Returns a number within the given rules' range.
    int nextSecretNumber(GameRules rules);
}
