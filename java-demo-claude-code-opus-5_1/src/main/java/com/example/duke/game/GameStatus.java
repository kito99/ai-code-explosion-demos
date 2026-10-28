package com.example.duke.game;

/// Where a game stands: still playable, or over because the player won or ran out of guesses.
public enum GameStatus {

    IN_PROGRESS,
    WON,
    LOST;

    /// Whether the game accepts no further guesses.
    public boolean isFinished() {
        return this != IN_PROGRESS;
    }
}
