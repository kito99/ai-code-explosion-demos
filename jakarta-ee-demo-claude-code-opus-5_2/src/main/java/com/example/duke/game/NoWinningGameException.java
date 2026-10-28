package com.example.duke.game;

import com.example.duke.error.ApplicationException;
import com.example.duke.error.ErrorKind;

/// Thrown when a score is posted for a session whose game has not been won.
public class NoWinningGameException extends ApplicationException {

    public NoWinningGameException() {
        super(ErrorKind.NO_WINNING_GAME,
                "Only a win can go on the leaderboard. Win a game and your score can be posted.");
    }
}
