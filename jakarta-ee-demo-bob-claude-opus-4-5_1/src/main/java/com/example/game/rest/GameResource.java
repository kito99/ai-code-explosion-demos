package com.example.game.rest;

import com.example.game.model.GameState;
import com.example.game.model.GameStatus;
import com.example.game.service.GameException;
import com.example.game.service.GameService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for game session management.
 *
 * - `POST /api/game`               — starts a new game
 * - `POST /api/game/{id}/guess`    — submits a guess
 */
@Path("/game")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameResource {

    @Inject
    GameService gameService;

    @POST
    public Response newGame() {
        var state = gameService.newGame();
        return Response.ok(state).build();
    }

    @POST
    @Path("/{sessionId}/guess")
    public Response guess(
            @PathParam("sessionId") String sessionId,
            GuessRequest request) {

        if (request == null || request.number() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Please enter a number between 1 and 10."))
                    .build();
        }

        int number = request.number();
        if (number < GameService.MIN_NUMBER || number > GameService.MAX_NUMBER) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(
                            "Please enter a whole number from 1 to 10."))
                    .build();
        }

        try {
            var state = gameService.guess(sessionId, number);
            return Response.ok(state).build();
        } catch (GameException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(ex.getMessage()))
                    .build();
        }
    }

    // --- Nested DTOs ---

    public record GuessRequest(Integer number) {}
    public record ErrorResponse(String message) {}
}
