package com.example.duke.web;

import com.example.duke.game.GameAlreadyFinishedException;
import com.example.duke.game.GameNotFoundException;
import com.example.duke.game.GameNotWonException;
import com.example.duke.game.InvalidGuessException;
import com.example.duke.game.ScoreAlreadyPostedException;
import com.example.duke.leaderboard.InvalidPlayerNameException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/// Turns the ways a request can be refused into friendly, machine-readable problem responses.
///
/// Messages say what happened and what the player can do next, and carry no personal data.
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String GUESS_HELP = "Please enter a whole number between 1 and 10.";

    private static final String REQUEST_HELP = "Please check what you sent and try again.";

    private static final String GUESSES_PATH = "/guesses";

    private static final String GAME_LOST_TITLE = "Duke lost track of that game";

    private static final String GAME_LOST_HELP = "Start a new game and Duke will think of another number.";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidGuessException.class)
    public ProblemDetail onInvalidGuess(final InvalidGuessException exception) {
        return problem(HttpStatus.BAD_REQUEST, "That guess is out of range", GUESS_HELP);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class})
    public ProblemDetail onUnreadableRequest(final Exception exception, final HttpServletRequest request) {
        LOGGER.debug("Rejected a malformed request", exception);
        final var help = request.getRequestURI().endsWith(GUESSES_PATH) ? GUESS_HELP : REQUEST_HELP;
        return problem(HttpStatus.BAD_REQUEST, "That is not something Duke can read", help);
    }

    /// A game id that is not a UUID cannot match a game, so the player gets the same answer as
    /// for an id the server no longer holds.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail onUnreadableGameId(final MethodArgumentTypeMismatchException exception) {
        LOGGER.debug("Rejected an unreadable game id", exception);
        return problem(HttpStatus.NOT_FOUND, GAME_LOST_TITLE, GAME_LOST_HELP);
    }

    @ExceptionHandler(InvalidPlayerNameException.class)
    public ProblemDetail onInvalidPlayerName(final InvalidPlayerNameException exception) {
        return problem(HttpStatus.BAD_REQUEST, "That name will not work", exception.getMessage());
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ProblemDetail onGameNotFound(final GameNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, GAME_LOST_TITLE, GAME_LOST_HELP);
    }

    @ExceptionHandler(GameAlreadyFinishedException.class)
    public ProblemDetail onGameAlreadyFinished(final GameAlreadyFinishedException exception) {
        return problem(HttpStatus.CONFLICT, "That game is over",
                "Start a new game to keep playing.");
    }

    @ExceptionHandler(GameNotWonException.class)
    public ProblemDetail onGameNotWon(final GameNotWonException exception) {
        return problem(HttpStatus.CONFLICT, "Only wins go on the leaderboard",
                "Win a game and Duke will ask for your name.");
    }

    @ExceptionHandler(ScoreAlreadyPostedException.class)
    public ProblemDetail onScoreAlreadyPosted(final ScoreAlreadyPostedException exception) {
        return problem(HttpStatus.CONFLICT, "That win is already on the leaderboard",
                "Start a new game to post another score.");
    }

    /// The leaderboard is the only part of the game that needs the database, so an unreachable
    /// one is reported as temporary rather than as a failed game. A database that is down fails
    /// when the transaction opens, which is a `TransactionException` rather than a
    /// `DataAccessException`, so both are answered the same way.
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ProblemDetail onDatabaseUnreachable(final RuntimeException exception) {
        LOGGER.error("The leaderboard database could not be reached", exception);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "The leaderboard is out of reach",
                "Your game is safe. Please try posting your score again in a moment.");
    }

    private static ProblemDetail problem(final HttpStatus status, final String title, final String detail) {
        final var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
