package com.example.guess.rest;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps {@link ConstraintViolationException} to a 400 response with a friendly message. FR-6, FR-14.
 */
@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                           .map(cv -> cv.getMessage())
                           .collect(Collectors.joining("; "));
        return Response.status(Response.Status.BAD_REQUEST)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(Map.of("message", message))
                       .build();
    }
}
