package tech.virtua.duke.game;

/// Source of the number Duke thinks of at the start of a game.
@FunctionalInterface
public interface SecretNumberPicker {

    /// Draws a number from [GameRules#LOWEST_NUMBER] through [GameRules#HIGHEST_NUMBER].
    int pickSecretNumber();
}
