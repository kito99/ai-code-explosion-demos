package com.example.duke.game;

/// Thrown when a game's score is posted to the leaderboard more than once.
public class ScoreAlreadyPostedException extends GameException {

    public ScoreAlreadyPostedException() {
        super("This win has already been posted to the leaderboard");
    }
}
