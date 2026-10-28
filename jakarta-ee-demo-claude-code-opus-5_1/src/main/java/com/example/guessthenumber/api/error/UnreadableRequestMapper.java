package com.example.guessthenumber.api.error;

import com.example.guessthenumber.api.dto.MessageResponse;
import com.example.guessthenumber.error.ErrorKind;
import com.example.guessthenumber.telemetry.GameMetrics;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/// Answers a request body that is not the JSON the endpoint expects, such as a guess sent
/// as text instead of a number, with a refusal rather than a server error.
@Provider
public class UnreadableRequestMapper implements ExceptionMapper<JsonbException> {

    @Inject
    GameMetrics metrics;

    @Override
    public Response toResponse(final JsonbException exception) {
        metrics.apiError(ErrorKind.INVALID_INPUT);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new MessageResponse(
                        "We could not read that request. Please send a whole number and try again."))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
