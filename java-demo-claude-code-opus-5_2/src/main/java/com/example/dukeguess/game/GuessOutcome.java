package com.example.dukeguess.game;

/// The result of one accepted guess: how it compared, and where the round stands afterwards.
public record GuessOutcome(GuessVerdict verdict, GameSnapshot game) {
}
