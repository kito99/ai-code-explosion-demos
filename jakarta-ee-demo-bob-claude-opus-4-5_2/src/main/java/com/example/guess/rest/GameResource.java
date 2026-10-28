package com.example.guess.rest;

import com.example.guess.game.GameSession;
import com.example.guess.game.GameSessionStore;
import com.example.guess.model.GameState;
import com.example.guess.model.GuessResult;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST endpoints for the game lifecycle.
 */
@Path("/game")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Game", description = "Game lifecycle operations")
public class GameResource {

    @Inject
    private GameSessionStore sessionStore;

    @Inject
    private Meter meter;

    private LongCounter gamesStartedCounter;
    private LongCounter gamesWonCounter;
    private LongCounter gamesLostCounter;

    @PostConstruct
    void initMetrics() {
        gamesStartedCounter = meter.counterBuilder("game.started")
                .setDescription("Number of games started")
                .build();
        gamesWonCounter = meter.counterBuilder("game.won")
                .setDescription("Number of games won")
                .build();
        gamesLostCounter = meter.counterBuilder("game.lost")
                .setDescription("Number of games lost")
                .build();
    }

    @POST
    @Path("/start")
    @Operation(summary = "Start a new game",
               description = "Creates a new game session and returns the session ID.")
    @APIResponse(responseCode = "201", description = "Game started",
                 content = @Content(schema = @Schema(implementation = GameState.class)))
    public Response startGame() {
        var sessionId = sessionStore.startGame();
        gamesStartedCounter.add(1);
        var session = sessionStore.getSession(sessionId);
        return Response.status(Response.Status.CREATED)
                .entity(toGameState(session))
                .build();
    }

    @GET
    @Path("/{sessionId}")
    @Operation(summary = "Get current game state")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Game state",
                     content = @Content(schema = @Schema(implementation = GameState.class))),
        @APIResponse(responseCode = "404", description = "Session not found")
    })
    public Response getState(
            @Parameter(description = "Game session ID", required = true)
            @PathParam("sessionId") String sessionId) {

        var session = sessionStore.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Session not found."))
                    .build();
        }
        return Response.ok(toGameState(session)).build();
    }

    @POST
    @Path("/{sessionId}/guess")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Submit a guess",
               description = "Submits a number guess for the given session. " +
                             "Accepted values are integers from 1 to 10. " +
                             "Jakarta Validation rejects out-of-range values before the method runs.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Guess processed",
                     content = @Content(schema = @Schema(implementation = GuessResult.class))),
        @APIResponse(responseCode = "404", description = "Session not found"),
        @APIResponse(responseCode = "422", description = "Invalid input — value outside 1–10")
    })
    public Response submitGuess(
            @Parameter(description = "Game session ID", required = true)
            @PathParam("sessionId") String sessionId,
            @RequestBody(description = "The guess value", required = true,
                         content = @Content(schema = @Schema(implementation = GuessRequest.class)))
            @Valid GuessRequest request) {

        var result = sessionStore.submitGuess(sessionId, request.guess());
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Session not found."))
                    .build();
        }

        if (result.feedback() == GuessResult.Feedback.CORRECT) {
            gamesWonCounter.add(1);
        } else if (result.feedback() == GuessResult.Feedback.GAME_OVER) {
            var session = sessionStore.getSession(sessionId);
            if (session != null && session.isLost()) {
                gamesLostCounter.add(1);
            }
        }

        return Response.ok(result).build();
    }

    private static GameState toGameState(GameSession session) {
        return new GameState(
                session.id(),
                session.guessCount(),
                session.guessesRemaining(),
                session.isActive(),
                session.isWon(),
                session.isLost());
    }
}
