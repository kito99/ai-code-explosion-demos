package com.example.duke.leaderboard;

/** Thrown when a player name fails validation rules (FR-14). */
public class InvalidPlayerNameException extends RuntimeException {

    public InvalidPlayerNameException(String message) {
        super(message);
    }
}
