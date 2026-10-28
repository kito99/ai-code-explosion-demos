package com.example.dukeguess.game;

/// Raised when a submitted guess is not a whole number within the playable range.
public class InvalidGuessException extends GameException {

    public InvalidGuessException(final int submitted) {
        super(String.format("%d is out of range. %s", submitted, GameRules.guessRangeMessage()));
    }
}
