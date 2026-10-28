package com.example.duke.game;

import com.example.duke.error.ApplicationException;
import com.example.duke.error.ErrorKind;

/// Thrown when a session tries to post the score for a win it has already posted.
public class ScoreAlreadyPostedException extends ApplicationException {

    public ScoreAlreadyPostedException() {
        super(ErrorKind.SCORE_ALREADY_POSTED,
                "This win is already on the leaderboard. Play again for another shot at the top.");
    }
}
