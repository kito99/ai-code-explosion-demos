package com.example.duke.game;

import com.example.duke.error.ApplicationException;
import com.example.duke.error.ErrorKind;

/// Thrown when a request needs a game and the session does not have one, which happens when
/// the session is new or has expired.
public class NoGameInProgressException extends ApplicationException {

    public NoGameInProgressException() {
        super(ErrorKind.NO_GAME_IN_PROGRESS,
                "There is no game going right now. Start a new one to play.");
    }
}
