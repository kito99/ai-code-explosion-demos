package com.example.duke.game;

/// Thrown when a score is posted for a game the player did not win.
public class GameNotWonException extends GameException {

    public GameNotWonException(final GameStatus status) {
        super("Only a won game can be posted to the leaderboard, and this one is " + status);
    }
}
