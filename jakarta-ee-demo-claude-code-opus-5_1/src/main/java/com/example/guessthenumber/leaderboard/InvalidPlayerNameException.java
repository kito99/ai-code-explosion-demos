package com.example.guessthenumber.leaderboard;

import com.example.guessthenumber.error.ApplicationException;
import com.example.guessthenumber.error.ErrorKind;

/// Thrown when a submitted name is blank or longer than the leaderboard allows.
public class InvalidPlayerNameException extends ApplicationException {

    public InvalidPlayerNameException() {
        super(ErrorKind.INVALID_INPUT,
                "Please enter a name of 1 to %d characters so Duke knows who to credit."
                        .formatted(PlayerName.MAX_LENGTH));
    }
}
