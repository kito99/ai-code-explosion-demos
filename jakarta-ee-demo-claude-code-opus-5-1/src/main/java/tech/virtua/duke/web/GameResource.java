package tech.virtua.duke.web;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.virtua.duke.game.SubmissionOutcome;

import static tech.virtua.duke.game.SubmissionOutcome.NONE;

/// The game's HTTP surface. Every endpoint answers with the whole game, so the page
/// renders whatever comes back without keeping any rules of its own.
///
/// Dependent scope is what lets this be a CDI bean, which is how it gets a constructor
/// rather than being built by the REST runtime; the runtime still decides its lifetime.
@Path("game")
@Dependent
@Produces(MediaType.APPLICATION_JSON)
public final class GameResource {

    private final PlayerGame playerGame;

    @Inject
    public GameResource(final PlayerGame playerGame) {
        this.playerGame = playerGame;
    }

    /// The game in play, dealing one if this browser has not played yet.
    @GET
    public GameView currentGame() {
        return GameView.of(playerGame.current(), NONE);
    }

    /// Abandons whatever was in play and deals a fresh game.
    @POST
    @Path("new")
    public GameView newGame() {
        return GameView.of(playerGame.startNewGame(), NONE);
    }

    /// Offers the game a guess. The answer says whether it counted.
    @POST
    @Path("guess")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response guess(final GuessRequest request) {
        final var submission = playerGame.submit(request == null ? null : request.guess());
        return Response.status(statusFor(submission.outcome()))
                .entity(GameView.of(submission.snapshot(), submission.outcome()))
                .build();
    }

    private static Response.Status statusFor(final SubmissionOutcome outcome) {
        return switch (outcome) {
            case NONE, COUNTED -> Response.Status.OK;
            case NOT_A_VALID_NUMBER -> Response.Status.BAD_REQUEST;
            case GAME_ALREADY_OVER -> Response.Status.CONFLICT;
        };
    }
}
