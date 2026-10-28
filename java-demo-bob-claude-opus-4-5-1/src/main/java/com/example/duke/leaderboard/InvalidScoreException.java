package com.example.duke.leaderboard;

/** Thrown when a score submission is rejected because the game was not won. */
public class InvalidScoreException extends RuntimeException {

    public InvalidScoreException(String message) {
        super(message);
    }
}
