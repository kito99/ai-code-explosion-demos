package com.example.guessduke.game;

/// Where a round stands: still being played, or finished one of two ways.
public enum GameStatus {

    /// Guesses are still being accepted.
    ACTIVE,

    /// The player found the secret number.
    WON,

    /// The player used every guess without finding the secret number.
    LOST;

    /// Reports whether the round has finished, either way.
    public boolean isFinished() {
        return this != ACTIVE;
    }
}
