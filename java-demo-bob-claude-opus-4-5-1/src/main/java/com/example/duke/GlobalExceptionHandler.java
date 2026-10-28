package com.example.duke;

import com.example.duke.game.GameAlreadyEndedException;
import com.example.duke.game.GameNotFoundException;
import com.example.duke.leaderboard.InvalidPlayerNameException;
import com.example.duke.leaderboard.InvalidScoreException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to HTTP responses with RFC 9457 Problem Detail bodies. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleGameNotFound(GameNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Game Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(GameAlreadyEndedException.class)
    public ResponseEntity<ProblemDetail> handleGameEnded(GameAlreadyEndedException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Game Already Ended");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidScoreException.class)
    public ResponseEntity<ProblemDetail> handleInvalidScore(InvalidScoreException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Cannot Post Score");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidPlayerNameException.class)
    public ResponseEntity<ProblemDetail> handleInvalidName(InvalidPlayerNameException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Name");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        var firstError = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("Validation failed");
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, firstError);
        problem.setTitle("Invalid Request");
        return ResponseEntity.badRequest().body(problem);
    }
}
