package com.example.dukeguess.game;

/// Supplies the secret number Duke thinks of at the start of a game.
@FunctionalInterface
public interface SecretNumberGenerator {

    /// Returns a number in `[lowestNumber, highestNumber]`, both inclusive.
    int nextSecretNumber(int lowestNumber, int highestNumber);
}
