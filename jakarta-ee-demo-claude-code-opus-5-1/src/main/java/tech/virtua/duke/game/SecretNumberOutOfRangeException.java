package tech.virtua.duke.game;

import static java.lang.String.format;

/// Thrown when a game is dealt a secret number no player could ever guess.
public class SecretNumberOutOfRangeException extends RuntimeException {

    public SecretNumberOutOfRangeException(final int secretNumber) {
        super(format("Secret number %d is outside %d to %d.",
                secretNumber, GameRules.LOWEST_NUMBER, GameRules.HIGHEST_NUMBER));
    }
}
