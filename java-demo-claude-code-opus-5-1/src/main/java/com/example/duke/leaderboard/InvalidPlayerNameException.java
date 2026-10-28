package com.example.duke.leaderboard;

/// Thrown when a posted name does not meet the leaderboard's name rules.
public class InvalidPlayerNameException extends RuntimeException {

    public InvalidPlayerNameException(final String message) {
        super(message);
    }
}
