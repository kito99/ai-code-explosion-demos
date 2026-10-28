package com.example.guessthenumber.game;

import com.example.guessthenumber.error.ApplicationException;
import com.example.guessthenumber.error.ErrorKind;

/// Thrown when a guess arrives after the game was won or lost.
public class GameAlreadyOverException extends ApplicationException {

    public GameAlreadyOverException() {
        super(ErrorKind.CONFLICT, "This game is already over. Start a new game to keep playing.");
    }
}
