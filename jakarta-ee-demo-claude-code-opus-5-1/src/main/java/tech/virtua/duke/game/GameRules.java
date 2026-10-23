package tech.virtua.duke.game;

/// The fixed numbers every game is played by.
public final class GameRules {

    /// The smallest number Duke can be thinking of.
    public static final int LOWEST_NUMBER = 1;

    /// The largest number Duke can be thinking of.
    public static final int HIGHEST_NUMBER = 10;

    /// How many guesses a player gets before the game is lost.
    public static final int GUESS_LIMIT = 5;

    private GameRules() {
    }

    /// Reports whether a number is one a player is allowed to guess.
    public static boolean isGuessable(final int number) {
        return number >= LOWEST_NUMBER && number <= HIGHEST_NUMBER;
    }
}
