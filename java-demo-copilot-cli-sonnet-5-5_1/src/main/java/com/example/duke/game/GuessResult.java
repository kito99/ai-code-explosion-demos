package com.example.duke.game;

/// The effect of one accepted guess.
///
/// @param outcome how the guess compared with the secret
/// @param game    a snapshot of the game after the guess
public record GuessResult(GuessOutcome outcome, GameSnapshot game) {
}
