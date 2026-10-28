package com.example.guess.rest;

import com.example.guess.game.GameSession;
import com.example.guess.game.GameSessionBean;
import com.example.guess.game.GuessResult;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * REST resource for game operations.
 *
 * POST /api/game/start  — start a new game (FR-1, FR-12)
 * GET  /api/game/state  — current game state
 * POST /api/game/guess  — submit a guess (FR-5, FR-6, FR-7, FR-8)
 */
@Path("/game")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GameResource {

    @Inject
    GameSessionBean sessionBean;

    /** FR-1, FR-12: start or restart a game. */
    @POST
    @Path("/start")
    public Response start() {
        GameSession session = sessionBean.newGame();
        return Response.ok(buildState(session)).build();
    }

    /** Returns the current game state without modifying it. */
    @GET
    @Path("/state")
    public Response state() {
        GameSession session = sessionBean.current();
        return Response.ok(buildState(session)).build();
    }

    /**
     * FR-5, FR-6, FR-7, FR-8: submit a guess.
     * Jakarta Validation fires on the {@link GuessRequest} parameter automatically.
     */
    @POST
    @Path("/guess")
    public Response guess(@Valid GuessRequest req) {
        GameSession session = sessionBean.current();
        if (!session.isActive()) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("message", "Game is not active. Start a new game first."))
                           .build();
        }
        GuessResult result = session.guess(req.getGuess());
        return Response.ok(buildGuessResult(result)).build();
    }

    // ---- helpers ----

    private Map<String, Object> buildState(GameSession s) {
        return Map.of(
                "active",           s.isActive(),
                "won",              s.isWon(),
                "guessesMade",      s.getGuessesMade(),
                "guessesRemaining", s.isActive() ? GameSession.MAX_GUESSES - s.getGuessesMade() : 0
        );
    }

    private Map<String, Object> buildGuessResult(GuessResult r) {
        return Map.of(
                "feedback",         r.feedback().name(),
                "guessesMade",      r.guessesMade(),
                "guessesRemaining", r.guessesRemaining(),
                "secretNumber",     r.secretNumber()   // 0 when active; revealed on win/lose
        );
    }
}
