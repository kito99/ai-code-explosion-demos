package com.example.dukeguess.rest;

import com.example.dukeguess.game.GuessOutOfRangeException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/// Explains a guess that was not a whole number in the playable range.
@Provider
public class GuessOutOfRangeExceptionMapper implements ExceptionMapper<GuessOutOfRangeException> {

    @Override
    public Response toResponse(final GuessOutOfRangeException exception) {
        return FriendlyResponses.of(BAD_REQUEST, exception.getMessage());
    }
}
