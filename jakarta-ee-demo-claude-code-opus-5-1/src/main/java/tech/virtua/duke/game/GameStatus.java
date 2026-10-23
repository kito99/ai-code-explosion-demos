package tech.virtua.duke.game;

/// Where a game stands.
public enum GameStatus {

    /// Guesses are still being accepted.
    ACTIVE,

    /// The player found the secret number.
    WON,

    /// The player ran out of guesses.
    LOST;

    /// Reports whether the game has finished, in a win or a loss.
    public boolean isOver() {
        return this != ACTIVE;
    }
}
