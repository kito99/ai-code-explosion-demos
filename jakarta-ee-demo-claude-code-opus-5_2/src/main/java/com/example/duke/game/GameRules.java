package com.example.duke.game;

/// The fixed rules of a round: the range Duke draws his secret number from and how many
/// guesses a player gets.
public final class GameRules {

    public static final int LOWEST_NUMBER = 1;
    public static final int HIGHEST_NUMBER = 10;
    public static final int MAX_GUESSES = 5;

    private GameRules() {
    }

    public static boolean isWithinRange(final int guess) {
        return guess >= LOWEST_NUMBER && guess <= HIGHEST_NUMBER;
    }
}
