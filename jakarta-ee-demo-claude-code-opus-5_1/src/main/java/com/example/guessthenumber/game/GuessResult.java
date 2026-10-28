package com.example.guessthenumber.game;

/// How a guess turned out, together with the game it was played against.
public record GuessResult(GuessOutcome outcome, Game game) {
}
