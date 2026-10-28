package com.example.dukeguess.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;

/// Explains that there is no win waiting to be posted.
@Provider
public class NoPostableWinExceptionMapper implements ExceptionMapper<NoPostableWinException> {

    @Override
    public Response toResponse(final NoPostableWinException exception) {
        return FriendlyResponses.of(CONFLICT, exception.getMessage());
    }
}
