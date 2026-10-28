package com.example.guessthenumber.game;

import com.example.guessthenumber.error.ApplicationException;
import com.example.guessthenumber.error.ErrorKind;

/// Thrown when a win that is already on the leaderboard is posted again, so a repeated
/// submission cannot add a second entry for the same game.
public class ScoreAlreadyPostedException extends ApplicationException {

    public ScoreAlreadyPostedException() {
        super(ErrorKind.CONFLICT, "This win is already on the leaderboard. Start a new game to post another score.");
    }
}
