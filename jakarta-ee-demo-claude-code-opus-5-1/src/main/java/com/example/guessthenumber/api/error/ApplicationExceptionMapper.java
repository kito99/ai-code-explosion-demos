package com.example.guessthenumber.api.error;

import com.example.guessthenumber.api.dto.MessageResponse;
import com.example.guessthenumber.error.ApplicationException;
import com.example.guessthenumber.error.ErrorKind;
import com.example.guessthenumber.telemetry.GameMetrics;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;

/// Turns an expected application error into the matching HTTP status and a sentence the
/// player can act on.
@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

    private static final Logger LOGGER = Logger.getLogger(ApplicationExceptionMapper.class.getName());

    @Inject
    GameMetrics metrics;

    @Override
    public Response toResponse(final ApplicationException exception) {
        final var kind = exception.kind();
        metrics.apiError(kind);
        LOGGER.fine(() -> "api.refused kind=%s type=%s".formatted(kind, exception.getClass().getSimpleName()));
        return Response.status(statusFor(kind))
                .entity(new MessageResponse(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response.Status statusFor(final ErrorKind kind) {
        return switch (kind) {
            case INVALID_INPUT -> Response.Status.BAD_REQUEST;
            case CONFLICT -> Response.Status.CONFLICT;
            case NOT_FOUND -> Response.Status.NOT_FOUND;
            case INTERNAL -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }
}
