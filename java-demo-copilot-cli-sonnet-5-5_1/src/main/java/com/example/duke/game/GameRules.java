package com.example.duke.game;

public final class GameRules {

    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 10;
    public static final int MAX_GUESSES = 5;

    private GameRules() {
    }

    public static boolean isInRange(final int number) {
        return number >= MIN_NUMBER && number <= MAX_NUMBER;
    }
}
