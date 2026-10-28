package com.example.guessthenumber.game;

import com.example.guessthenumber.error.ApplicationException;
import com.example.guessthenumber.error.ErrorKind;

/// Thrown when a score is posted without a won game behind it.
public class NoWinningGameException extends ApplicationException {

    public NoWinningGameException() {
        super(ErrorKind.CONFLICT, "There is no win to post yet. Win a game first, then post your score.");
    }
}
