package com.example.duke.game;

import static java.lang.String.format;

/// Thrown when a guess arrives for a game that has already been won or lost.
public class GameAlreadyFinishedException extends GameException {

    public GameAlreadyFinishedException(final GameStatus status) {
        super(format("This game is already over (%s)", status));
    }
}
