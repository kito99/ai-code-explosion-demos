package com.example.duke.game;

public class InvalidGuessException extends GameException {

    public InvalidGuessException() {
        super(String.format("Please enter a whole number from %d to %d.",
                GameRules.MIN_NUMBER, GameRules.MAX_NUMBER));
    }
}
