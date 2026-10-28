package com.example.duke.game;

public class GameFinishedException extends GameException {

    public GameFinishedException() {
        super("This game is over. Start a new game to play again.");
    }
}
