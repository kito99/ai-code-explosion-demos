package com.example.duke.game;

import com.example.duke.error.ApplicationException;
import com.example.duke.error.ErrorKind;

/// Thrown when a guess arrives after the round has already been won or lost.
public class GameAlreadyOverException extends ApplicationException {

    public GameAlreadyOverException() {
        super(ErrorKind.GAME_ALREADY_OVER,
                "This game is over. Start a new one to keep playing.");
    }
}
