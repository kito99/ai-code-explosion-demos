package com.example.dukeguess.rest;

import com.example.dukeguess.game.GameAlreadyOverException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;

/// Explains that the round is finished when a late guess arrives.
@Provider
public class GameAlreadyOverExceptionMapper implements ExceptionMapper<GameAlreadyOverException> {

    @Override
    public Response toResponse(final GameAlreadyOverException exception) {
        return FriendlyResponses.of(CONFLICT, exception.getMessage());
    }
}
