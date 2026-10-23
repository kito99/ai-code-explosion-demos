package tech.virtua.duke.web;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/// Answers a request body the server cannot read with the same friendly rejection a
/// player gets for typing nonsense, so the page never has to render a container error.
///
/// The REST runtime builds its own providers and only knows how to call a no-argument
/// constructor, so the game arrives by field injection rather than through one.
@Provider
@Dependent
public final class UnreadableGuessMapper implements ExceptionMapper<JsonbException> {

    @Inject
    private PlayerGame playerGame;

    @Override
    public Response toResponse(final JsonbException exception) {
        final var submission = playerGame.submit(null);
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(GameView.of(submission.snapshot(), submission.outcome()))
                .build();
    }
}
