package com.example.duke.api.error;

import com.example.duke.api.dto.ErrorResponse;
import com.example.duke.error.ErrorKind;
import com.example.duke.telemetry.GameTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/// Handles a body JSON Binding cannot read — a guess sent as text, say. That fails while the
/// request is being deserialized, so it never reaches a constraint and would otherwise look
/// like an internal failure.
@Provider
@ApplicationScoped
public class UnreadableRequestMapper implements ExceptionMapper<JsonbException> {

    private static final String MESSAGE =
            "That request could not be read. Send a whole number from 1 to 10 as the guess.";

    @Inject
    private GameTelemetry telemetry;

    @Override
    public Response toResponse(final JsonbException unreadable) {
        telemetry.apiError(ErrorKind.INVALID_INPUT);
        return Response.status(Status.BAD_REQUEST)
                .entity(new ErrorResponse(ErrorKind.INVALID_INPUT, MESSAGE))
                .type(APPLICATION_JSON)
                .build();
    }
}
