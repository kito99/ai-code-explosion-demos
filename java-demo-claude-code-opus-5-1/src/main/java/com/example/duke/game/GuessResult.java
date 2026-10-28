package com.example.duke.game;

/// The state a game reached after a guess, together with how that guess compared.
public record GuessResult(Game game, GuessOutcome outcome) {
}
