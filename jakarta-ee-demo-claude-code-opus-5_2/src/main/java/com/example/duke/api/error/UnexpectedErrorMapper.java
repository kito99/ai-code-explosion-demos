package com.example.duke.api.error;

import com.example.duke.api.dto.ErrorResponse;
import com.example.duke.error.ErrorKind;
import com.example.duke.telemetry.GameTelemetry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/// The last resort for anything the application did not anticipate. The cause is logged in
/// full and counted, while the caller gets a short message with no internal detail in it.
@Provider
@ApplicationScoped
public class UnexpectedErrorMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UnexpectedErrorMapper.class.getName());

    private static final String MESSAGE =
            "Something went wrong on our end. Start a new game and try again.";

    @Inject
    private GameTelemetry telemetry;

    @Override
    public Response toResponse(final Throwable failure) {
        // Jakarta REST routes its own 404s and the like through here too; those already carry
        // the response they should produce, so they are passed along untouched.
        if (failure instanceof WebApplicationException carriesItsOwnResponse) {
            return carriesItsOwnResponse.getResponse();
        }

        telemetry.apiError(ErrorKind.UNEXPECTED);
        LOG.log(Level.SEVERE, "Unexpected failure while handling a request", failure);
        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(ErrorKind.UNEXPECTED, MESSAGE))
                .type(APPLICATION_JSON)
                .build();
    }
}
