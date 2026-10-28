package com.example.guessthenumber.api.error;

import com.example.guessthenumber.api.dto.MessageResponse;
import com.example.guessthenumber.telemetry.GameMetrics;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.guessthenumber.error.ErrorKind.INTERNAL;

/// Last resort for failures the application did not anticipate: logs the cause with its
/// stack trace so the telemetry shows where the error came from, and hands the player a
/// sentence instead of a stack trace.
@Provider
public class UnexpectedErrorMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(UnexpectedErrorMapper.class.getName());

    @Inject
    GameMetrics metrics;

    @Override
    public Response toResponse(final Throwable failure) {
        // Jakarta REST routes its own 404s and 405s through this mapper too; those already
        // carry the right response, so they pass through untouched.
        if (failure instanceof WebApplicationException webFailure) {
            return webFailure.getResponse();
        }

        metrics.apiError(INTERNAL);
        LOGGER.log(Level.SEVERE, "api.failed unexpectedly", failure);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new MessageResponse(
                        "Something went wrong on Duke's side. Please start a new game and try again."))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
