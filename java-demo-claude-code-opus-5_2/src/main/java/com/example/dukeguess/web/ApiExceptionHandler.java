package com.example.dukeguess.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.dukeguess.game.GameAlreadyOverException;
import com.example.dukeguess.game.GameNotFoundException;
import com.example.dukeguess.game.GameNotWonException;
import com.example.dukeguess.game.GameRules;
import com.example.dukeguess.game.InvalidGuessException;
import com.example.dukeguess.game.ScoreAlreadyPostedException;
import com.example.dukeguess.leaderboard.InvalidPlayerNameException;

/// Turns a rejected request into a problem response carrying the same friendly wording
/// the player sees on screen.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String UNREADABLE_DETAIL =
            "Send a JSON body matching the API definition, then try again.";

    @ExceptionHandler(InvalidGuessException.class)
    public ProblemDetail onInvalidGuess(final InvalidGuessException exception) {
        return ApiProblem.INVALID_GUESS.asProblemDetail(exception.getMessage());
    }

    @ExceptionHandler(InvalidPlayerNameException.class)
    public ProblemDetail onInvalidName(final InvalidPlayerNameException exception) {
        return ApiProblem.INVALID_NAME.asProblemDetail(exception.getMessage());
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ProblemDetail onGameNotFound(final GameNotFoundException exception) {
        return ApiProblem.GAME_NOT_FOUND.asProblemDetail(exception.getMessage());
    }

    @ExceptionHandler(GameAlreadyOverException.class)
    public ProblemDetail onGameOver(final GameAlreadyOverException exception) {
        return ApiProblem.GAME_OVER.asProblemDetail(exception.getMessage());
    }

    @ExceptionHandler(GameNotWonException.class)
    public ProblemDetail onGameNotWon(final GameNotWonException exception) {
        return ApiProblem.GAME_NOT_WON.asProblemDetail(exception.getMessage());
    }

    @ExceptionHandler(ScoreAlreadyPostedException.class)
    public ProblemDetail onScoreAlreadyPosted(final ScoreAlreadyPostedException exception) {
        return ApiProblem.SCORE_ALREADY_POSTED.asProblemDetail(exception.getMessage());
    }

    /// A missing or non-numeric guess reaches here before the game ever sees it, so it
    /// answers with the same wording an out-of-range guess gets.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onMissingField() {
        LOGGER.debug("Rejected a request body that did not carry a usable guess");
        return ApiProblem.INVALID_GUESS.asProblemDetail(GameRules.guessRangeMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail onUnreadableBody() {
        LOGGER.debug("Rejected a request body that could not be parsed");
        return ApiProblem.MALFORMED_REQUEST.asProblemDetail(UNREADABLE_DETAIL);
    }

    /// The game itself needs no database, so an outage only blocks the leaderboard. The
    /// round the player is in keeps playing and the win stays postable once it is back.
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail onDatabaseFailure(final DataAccessException exception) {
        LOGGER.error("The leaderboard database turned down a request: {}", describe(exception));
        return ApiProblem.LEADERBOARD_UNAVAILABLE
                .asProblemDetail("The leaderboard is having a moment. Try again shortly.");
    }

    /// Names the failure by type rather than by message.
    ///
    /// A parser or database message can quote the value that upset it, and the only value
    /// these requests carry is the player's name, which NFR-9 keeps out of telemetry.
    private static String describe(final Throwable failure) {
        final var cause = NestedExceptionUtils.getMostSpecificCause(failure);
        return cause == failure ? failure.getClass().getName()
                : String.format("%s caused by %s", failure.getClass().getName(), cause.getClass().getName());
    }
}
