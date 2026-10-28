package com.example.guessthenumber.game;

import com.example.guessthenumber.error.ApplicationException;
import com.example.guessthenumber.error.ErrorKind;

/// Thrown when the player acts on a game that was never started.
public class NoGameInProgressException extends ApplicationException {

    public NoGameInProgressException() {
        super(ErrorKind.NOT_FOUND, "There is no game going right now. Start a new game to play.");
    }
}
