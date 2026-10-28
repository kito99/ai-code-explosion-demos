package com.example.dukeguess.leaderboard;

/// Raised when a submitted name is blank or longer than [PlayerName#MAX_LENGTH] characters.
public class InvalidPlayerNameException extends RuntimeException {

    public InvalidPlayerNameException() {
        super(String.format("Please enter a name of 1 to %d characters.", PlayerName.MAX_LENGTH));
    }
}
