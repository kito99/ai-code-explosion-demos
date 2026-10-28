package com.example.duke.game;

import com.example.duke.error.ApplicationException;
import com.example.duke.error.ErrorKind;

import static com.example.duke.game.GameRules.HIGHEST_NUMBER;
import static com.example.duke.game.GameRules.LOWEST_NUMBER;
import static java.lang.String.format;

/// Thrown when a guess falls outside the allowed range. The guess is not counted and the
/// guesses remaining do not change.
public class InvalidGuessException extends ApplicationException {

    public InvalidGuessException(final int guess) {
        super(ErrorKind.INVALID_INPUT, format(
                "%d is not between %d and %d, so it does not count as a guess. Try again.",
                guess, LOWEST_NUMBER, HIGHEST_NUMBER));
    }
}
