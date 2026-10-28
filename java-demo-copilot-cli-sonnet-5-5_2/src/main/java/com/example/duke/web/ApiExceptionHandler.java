package com.example.duke.web;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.duke.game.GameFinishedException;
import com.example.duke.game.GameNotFoundException;
import com.example.duke.game.InvalidGuessException;
import com.example.duke.game.ScoreNotPostableException;
import com.example.duke.leaderboard.InvalidNameException;

@RestControllerAdvice(assignableTypes = GameController.class)
public class ApiExceptionHandler {

    @ExceptionHandler({InvalidGuessException.class, InvalidNameException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput(final RuntimeException exception) {
        return respond(BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody() {
        return respond(BAD_REQUEST, "That request couldn't be read. Please check what you entered and try again.");
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final GameNotFoundException exception) {
        return respond(NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({GameFinishedException.class, ScoreNotPostableException.class})
    public ResponseEntity<ErrorResponse> handleConflict(final RuntimeException exception) {
        return respond(CONFLICT, exception.getMessage());
    }

    private static ResponseEntity<ErrorResponse> respond(final HttpStatus status, final String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
