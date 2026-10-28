package com.example.guessduke.game;

/// Supplies the number Duke is thinking of at the start of a round.
@FunctionalInterface
public interface SecretNumberGenerator {

    /// Draws the next secret number, which always falls within the playable range.
    int nextSecretNumber();
}
