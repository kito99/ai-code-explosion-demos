package com.example.duke.game;

/// The fixed rules of a round: the range Duke draws from and how many guesses a player gets.
public final class GameRules {

    /// Lowest number Duke can think of.
    public static final int LOWEST_NUMBER = 1;

    /// Highest number Duke can think of.
    public static final int HIGHEST_NUMBER = 10;

    /// Guesses allowed per game before the player loses.
    public static final int MAX_GUESSES = 5;

    private GameRules() {
    }

    /// Whether `number` is one Duke could have picked.
    public static boolean isWithinRange(final int number) {
        return number >= LOWEST_NUMBER && number <= HIGHEST_NUMBER;
    }
}
