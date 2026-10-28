package com.example.dukeguess.game;

/// Lifecycle states a game moves through.
public enum GameStatus {
    /// Guesses are still being accepted.
    ACTIVE,
    /// The secret number was found.
    WON,
    /// Every allowed guess was used without finding the secret number.
    LOST
}
