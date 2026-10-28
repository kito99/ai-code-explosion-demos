package com.example.duke.game;

/// Draws the number Duke is thinking of.
@FunctionalInterface
public interface SecretNumberGenerator {

    /// A number in the range [GameRules#LOWEST_NUMBER] to [GameRules#HIGHEST_NUMBER],
    /// inclusive, drawn independently of every previous draw.
    int draw();
}
