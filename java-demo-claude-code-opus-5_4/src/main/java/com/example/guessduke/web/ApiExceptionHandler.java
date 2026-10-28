package com.example.guessduke.web;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.guessduke.game.GameNotActiveException;
import com.example.guessduke.game.InvalidGuessException;

/// Turns failures into the same readable shape the rest of the API uses, so the
/// game screen always has a message to show. The gameplay services handle every
/// expected path themselves, so anything reaching here is a request the server
/// could not make sense of or a failure it could not recover from.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String UNREADABLE_REQUEST = "Duke didn't understand that request. Start a new game and try again.";

    private static final String STORAGE_UNAVAILABLE =
            "Duke couldn't reach the leaderboard just now. Give it a moment and try again.";

    @ExceptionHandler(GameNotActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleGameNotActive() {
        return new ApiError(GameMessages.ROUND_ALREADY_OVER);
    }

    @ExceptionHandler(InvalidGuessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidGuess() {
        return new ApiError(GameMessages.GUESS_NOT_ACCEPTED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadableRequest() {
        return new ApiError(UNREADABLE_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleStorageFailure() {
        return new ApiError(STORAGE_UNAVAILABLE);
    }
}
