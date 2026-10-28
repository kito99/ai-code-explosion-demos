package com.example.duke.game;

import static java.lang.String.format;

/// Thrown when a submitted guess is outside the range Duke draws from.
public class InvalidGuessException extends GameException {

    public InvalidGuessException(final int guess) {
        super(format("%d is not a whole number between %d and %d",
                guess, GameRules.LOWEST_NUMBER, GameRules.HIGHEST_NUMBER));
    }
}
