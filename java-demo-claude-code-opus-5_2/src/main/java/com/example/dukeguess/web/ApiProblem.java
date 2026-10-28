package com.example.dukeguess.web;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/// The ways an API call can be turned down, each with the status and heading it answers with.
///
/// The machine-readable `code` lets the browser tell these apart without matching on
/// message text.
public enum ApiProblem {

    INVALID_GUESS(HttpStatus.BAD_REQUEST, "That is not a guess Duke can score"),
    INVALID_NAME(HttpStatus.BAD_REQUEST, "That name will not fit on the leaderboard"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "That request could not be read"),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "That game is not in play"),
    GAME_OVER(HttpStatus.CONFLICT, "That game has already finished"),
    GAME_NOT_WON(HttpStatus.CONFLICT, "Only a won game can post a score"),
    SCORE_ALREADY_POSTED(HttpStatus.CONFLICT, "That win is already posted"),
    LEADERBOARD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The leaderboard is not reachable");

    private static final String CODE_PROPERTY = "code";

    private final HttpStatus status;
    private final String title;

    ApiProblem(final HttpStatus status, final String title) {
        this.status = status;
        this.title = title;
    }

    public ProblemDetail asProblemDetail(final String detail) {
        final var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty(CODE_PROPERTY, code());
        return problemDetail;
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
