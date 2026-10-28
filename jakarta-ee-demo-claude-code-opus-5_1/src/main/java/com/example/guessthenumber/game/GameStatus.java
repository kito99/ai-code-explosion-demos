package com.example.guessthenumber.game;

/// Where a game stands.
public enum GameStatus {
    /// The player still has guesses left and has not found the secret number.
    ACTIVE,

    /// The player found the secret number.
    WON,

    /// The player used every guess without finding the secret number.
    LOST;

    public boolean isFinished() {
        return this != ACTIVE;
    }
}
