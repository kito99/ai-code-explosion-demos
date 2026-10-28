package com.example.duke.game;

/// Where a round stands.
public enum GameStatus {

    /// The player still has guesses left and has not found the number.
    IN_PROGRESS,

    /// The player found the number.
    WON,

    /// The player used every guess without finding the number.
    LOST;

    public boolean isOver() {
        return this != IN_PROGRESS;
    }
}
