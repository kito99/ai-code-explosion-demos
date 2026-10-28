package com.example.guessthenumber.game;

import java.io.Serializable;

/// The bounds of the secret number and how many guesses a game allows.
public record GameRules(int lowestNumber, int highestNumber, int maxGuesses) implements Serializable {

    /// The only rules the game ships with; the range and the guess limit are fixed.
    public static final GameRules STANDARD = new GameRules(1, 10, 5);

    public GameRules {
        if (lowestNumber > highestNumber) {
            throw new IllegalArgumentException(
                    "lowestNumber %d must not exceed highestNumber %d".formatted(lowestNumber, highestNumber));
        }
        if (maxGuesses < 1) {
            throw new IllegalArgumentException("maxGuesses must be at least 1 but was %d".formatted(maxGuesses));
        }
    }

    public boolean allows(final int number) {
        return number >= lowestNumber && number <= highestNumber;
    }
}
