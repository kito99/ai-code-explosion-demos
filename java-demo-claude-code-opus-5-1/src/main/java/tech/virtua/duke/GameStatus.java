package tech.virtua.duke;

/// Where a game stands: still being played, or finished one way or the other.
public enum GameStatus {

    /// The player may still guess.
    ACTIVE,

    /// The player found the secret number.
    WON,

    /// The player used every guess without finding the secret number.
    LOST;

    /// Whether this status still accepts guesses.
    public boolean acceptsGuesses() {
        return this == ACTIVE;
    }

    /// Whether the secret number may be shown to the player.
    public boolean revealsSecret() {
        return this != ACTIVE;
    }
}
