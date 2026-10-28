package com.example.dukeguess.game;

/// The fixed rules of a round: the range Duke picks from and how many guesses a player gets.
public final class GameRules {

    public static final int LOWEST_NUMBER = 1;
    public static final int HIGHEST_NUMBER = 10;
    public static final int MAX_GUESSES = 5;

    private GameRules() {
    }

    public static boolean isGuessInRange(final int guess) {
        return guess >= LOWEST_NUMBER && guess <= HIGHEST_NUMBER;
    }

    /// Throws [InvalidGuessException] when the guess falls outside the playable range.
    public static void requireGuessInRange(final int guess) {
        if (!isGuessInRange(guess)) {
            throw new InvalidGuessException(guess);
        }
    }

    /// The wording every rejected guess comes back with, on screen and through the API.
    public static String guessRangeMessage() {
        return String.format("Please enter a whole number from %d to %d.", LOWEST_NUMBER, HIGHEST_NUMBER);
    }
}
