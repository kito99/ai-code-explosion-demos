package tech.virtua.guessthenumber.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.virtua.guessthenumber.GameSession;
import tech.virtua.guessthenumber.game.SubmitResult;

/**
 * The game's HTTP surface. All three endpoints answer with the same {@link GameView}
 * so the page can render any response the same way; the status code says whether a
 * submitted guess actually counted.
 */
@Path("/game")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class GameResource {

    @Inject
    private GameSession session;

    /** The game in play, starting one if this session has not played yet. */
    @GET
    public GameView currentGame() {
        return GameView.of(session.current());
    }

    /** Abandons the current game and deals a fresh secret number. */
    @POST
    public GameView newGame() {
        return GameView.of(session.startNewGame());
    }

    /**
     * Submits a guess. Answers 200 when the guess counted, 400 when the input was not
     * a whole number in range, and 409 when the game was already over. The body is the
     * resulting game state either way, and its message explains the outcome.
     */
    @POST
    @Path("/guesses")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submitGuess(GuessRequest request) {
        SubmitResult result = session.submit(request == null ? null : request.getGuess());

        return switch (result.acceptance()) {
            case ACCEPTED -> Response.ok(GameView.of(result.game())).build();
            case INVALID_INPUT -> Response.status(Response.Status.BAD_REQUEST)
                    .entity(GameView.of(result.game(), GameMessages.rejectInput(result.game())))
                    .build();
            case GAME_OVER -> Response.status(Response.Status.CONFLICT)
                    .entity(GameView.of(result.game()))
                    .build();
        };
    }
}
