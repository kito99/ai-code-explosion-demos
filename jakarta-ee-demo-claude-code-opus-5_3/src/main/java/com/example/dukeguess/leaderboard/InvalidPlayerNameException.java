package com.example.dukeguess.leaderboard;

/// Thrown when a submitted name does not meet the leaderboard's name rules.
public class InvalidPlayerNameException extends RuntimeException {

    public InvalidPlayerNameException(final String message) {
        super(message);
    }
}
