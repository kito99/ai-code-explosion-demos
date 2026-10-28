package com.example.duke.game;

/// Supplies the number Duke thinks of at the start of a round.
@FunctionalInterface
public interface SecretNumberGenerator {

    /// Returns a number in the range [GameRules#LOWEST_NUMBER] to [GameRules#HIGHEST_NUMBER], inclusive.
    int nextSecretNumber();
}
