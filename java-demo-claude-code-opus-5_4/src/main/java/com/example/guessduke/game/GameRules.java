package com.example.guessduke.game;

/// The fixed numbers that define a round: the range Duke draws from and how many
/// guesses a player gets.
public final class GameRules {

    /// Lowest number Duke can be thinking of.
    public static final int LOWEST_NUMBER = 1;

    /// Highest number Duke can be thinking of.
    public static final int HIGHEST_NUMBER = 10;

    /// Accepted guesses a player gets before the round is lost.
    public static final int MAX_GUESSES = 5;

    private GameRules() {
    }

    /// Reports whether a number is one Duke could be thinking of.
    public static boolean isPlayableNumber(final int number) {
        return number >= LOWEST_NUMBER && number <= HIGHEST_NUMBER;
    }
}
