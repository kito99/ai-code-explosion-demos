package tech.virtua.duke;

/// The fixed rules of the game. The business requirements put configurable
/// ranges and guess limits out of scope, so these are constants rather than
/// settings.
public final class GameRules {

    /// Lowest number the secret can be.
    public static final int LOWEST_NUMBER = 1;

    /// Highest number the secret can be.
    public static final int HIGHEST_NUMBER = 10;

    /// Accepted guesses allowed per game.
    public static final int GUESS_LIMIT = 5;

    private GameRules() {
    }

    /// Whether `candidate` is inside the guessable range.
    public static boolean isInRange(final int candidate) {
        return candidate >= LOWEST_NUMBER && candidate <= HIGHEST_NUMBER;
    }
}
