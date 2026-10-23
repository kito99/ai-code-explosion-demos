package tech.virtua.guessthenumber.rest;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.virtua.guessthenumber.game.GuessingGame;

/**
 * Keeps a request body the binder cannot read from surfacing as a server error.
 * Hand-written requests are the usual source, so the reply states the expected shape.
 */
@Provider
public class MalformedRequestMapper implements ExceptionMapper<JsonbException> {

    @Override
    public Response toResponse(JsonbException unreadableBody) {
        String message = "Send a guess as {\"guess\": \"<number>\"} with a whole number from "
                + GuessingGame.MIN_NUMBER + " to " + GuessingGame.MAX_NUMBER + ".";
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorView(message))
                .build();
    }

    /** The error shape, kept distinct from {@link GameView} so callers can tell them apart. */
    public record ErrorView(String message) {
    }
}
