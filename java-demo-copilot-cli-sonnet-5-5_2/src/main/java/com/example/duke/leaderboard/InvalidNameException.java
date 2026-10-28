package com.example.duke.leaderboard;

import com.example.duke.game.GameException;

public class InvalidNameException extends GameException {

    public InvalidNameException() {
        super(String.format("Please enter a name with 1 to %d characters.", LeaderboardService.MAX_NAME_LENGTH));
    }
}
