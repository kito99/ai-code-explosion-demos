package com.example.duke.api.error;

import com.example.duke.api.dto.ErrorResponse;
import com.example.duke.error.ApplicationException;
import com.example.duke.error.ErrorKind;
import com.example.duke.telemetry.GameTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/// Turns the failures the game reports into HTTP responses the player-facing message
/// survives, and counts each one so the telemetry shows where errors come from.
@Provider
@ApplicationScoped
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

    @Inject
    private GameTelemetry telemetry;

    @Override
    public Response toResponse(final ApplicationException failure) {
        telemetry.apiError(failure.kind());
        return Response.status(statusFor(failure.kind()))
                .entity(new ErrorResponse(failure.kind(), failure.getMessage()))
                .type(APPLICATION_JSON)
                .build();
    }

    private static Status statusFor(final ErrorKind kind) {
        return switch (kind) {
            case INVALID_INPUT -> Status.BAD_REQUEST;
            case NO_GAME_IN_PROGRESS, GAME_ALREADY_OVER, NO_WINNING_GAME, SCORE_ALREADY_POSTED ->
                    Status.CONFLICT;
            case UNEXPECTED -> Status.INTERNAL_SERVER_ERROR;
        };
    }
}
