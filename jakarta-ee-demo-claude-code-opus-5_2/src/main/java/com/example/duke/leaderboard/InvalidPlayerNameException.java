package com.example.duke.leaderboard;

import com.example.duke.error.ApplicationException;
import com.example.duke.error.ErrorKind;

/// Thrown when a name does not pass the leaderboard's name rules. Nothing is recorded until
/// an acceptable name arrives.
public class InvalidPlayerNameException extends ApplicationException {

    public InvalidPlayerNameException() {
        super(ErrorKind.INVALID_INPUT, PlayerName.RULE_MESSAGE);
    }
}
