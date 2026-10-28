package com.example.duke.api.error;

import com.example.duke.api.dto.ErrorResponse;
import com.example.duke.error.ErrorKind;
import com.example.duke.telemetry.GameTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/// Replaces the empty body Jakarta REST would otherwise return for a failed Jakarta
/// Validation check with the friendly message the constraint carries.
@Provider
@ApplicationScoped
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final String MESSAGE_SEPARATOR = " ";

    @Inject
    private GameTelemetry telemetry;

    @Override
    public Response toResponse(final ConstraintViolationException violations) {
        telemetry.apiError(ErrorKind.INVALID_INPUT);
        return Response.status(Status.BAD_REQUEST)
                .entity(new ErrorResponse(ErrorKind.INVALID_INPUT, messageFrom(violations)))
                .type(APPLICATION_JSON)
                .build();
    }

    private static String messageFrom(final ConstraintViolationException violations) {
        return violations.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining(MESSAGE_SEPARATOR));
    }
}
