package com.example.dukeguess.game;

/// Supplies the number Duke is thinking of at the start of a round.
@FunctionalInterface
public interface SecretNumberPicker {

    /// Returns a number within [GameRules#LOWEST_NUMBER] and [GameRules#HIGHEST_NUMBER], inclusive.
    int pick();
}
