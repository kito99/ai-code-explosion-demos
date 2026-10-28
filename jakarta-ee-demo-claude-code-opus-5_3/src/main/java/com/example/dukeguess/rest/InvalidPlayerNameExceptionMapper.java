package com.example.dukeguess.rest;

import com.example.dukeguess.leaderboard.InvalidPlayerNameException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/// Explains a name that does not meet the leaderboard's name rules.
@Provider
public class InvalidPlayerNameExceptionMapper implements ExceptionMapper<InvalidPlayerNameException> {

    @Override
    public Response toResponse(final InvalidPlayerNameException exception) {
        return FriendlyResponses.of(BAD_REQUEST, exception.getMessage());
    }
}
