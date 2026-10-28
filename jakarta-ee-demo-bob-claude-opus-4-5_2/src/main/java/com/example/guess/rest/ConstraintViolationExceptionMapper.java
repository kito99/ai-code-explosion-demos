package com.example.guess.rest;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps Jakarta Validation constraint violations to HTTP 422 responses,
 * collecting the first violation message into the standard error body.
 */
@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var message = exception.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("Invalid input.");
        return Response.status(422) // 422 Unprocessable Entity (not a named constant in Jakarta REST 4.0)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(message))
                .build();
    }
}
