package com.example.duke.web;

import com.example.duke.game.GameError;
import com.example.duke.game.GameException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static com.example.duke.game.GameRules.MAX_NUMBER;
import static com.example.duke.game.GameRules.MIN_NUMBER;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(GameException.class)
    ResponseEntity<ErrorResponse> handleGame(final GameException exception) {
        return ResponseEntity.status(statusOf(exception.error()))
                .body(new ErrorResponse(exception.error(), exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(final HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(GameError.INVALID_GUESS,
                "Please enter a whole number from %d to %d.".formatted(MIN_NUMBER, MAX_NUMBER)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleBadId(final MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(GameError.GAME_NOT_FOUND,
                "That game could not be found. Start a new game to play."));
    }

    private static HttpStatus statusOf(final GameError error) {
        return switch (error) {
            case INVALID_GUESS, INVALID_NAME -> HttpStatus.BAD_REQUEST;
            case GAME_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case GAME_FINISHED, GAME_NOT_WON, SCORE_ALREADY_POSTED -> HttpStatus.CONFLICT;
        };
    }
}
