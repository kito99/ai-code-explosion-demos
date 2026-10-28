package com.example.guessthenumber.game;

import com.example.guessthenumber.error.ApplicationException;
import com.example.guessthenumber.error.ErrorKind;

/// Thrown when a guess is missing or outside the allowed range. A rejected guess never
/// counts against the guess limit.
public class InvalidGuessException extends ApplicationException {

    public InvalidGuessException(final GameRules rules) {
        super(ErrorKind.INVALID_INPUT,
                "Please pick a whole number between %d and %d.".formatted(rules.lowestNumber(), rules.highestNumber()));
    }
}
