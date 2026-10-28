package com.example.dukeguess.rest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static java.util.stream.Collectors.joining;

/// Reports Jakarta Validation failures in the same friendly shape as the rest of the API.
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final String FALLBACK_MESSAGE = "That request did not look right. Please try again.";

    @Override
    public Response toResponse(final ConstraintViolationException exception) {
        final var message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(joining(" "));
        return FriendlyResponses.of(BAD_REQUEST, message.isBlank() ? FALLBACK_MESSAGE : message);
    }
}
