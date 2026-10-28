package com.example.duke.game;

public class GameNotFoundException extends GameException {

    public GameNotFoundException(final String gameId) {
        super(String.format("Duke can't find game %s. Start a new game to keep playing.", gameId));
    }
}
