package com.example.dukeguess.game;

/// Raised when a round's win has already been written to the leaderboard.
public class ScoreAlreadyPostedException extends GameException {

    public ScoreAlreadyPostedException() {
        super("This win is already on the leaderboard.");
    }
}
