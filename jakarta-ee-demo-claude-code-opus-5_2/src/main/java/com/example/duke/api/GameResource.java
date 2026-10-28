package com.example.duke.api;

import com.example.duke.api.dto.ErrorResponse;
import com.example.duke.api.dto.GameStateResponse;
import com.example.duke.api.dto.GuessRequest;
import com.example.duke.game.GameService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/// The game actions the screen performs. The game lives in the caller's HTTP session, so
/// none of these paths carry a game id.
@Path("/games")
@ApplicationScoped
@Produces(APPLICATION_JSON)
@Tag(name = "Game")
public class GameResource {

    private static final String CURRENT_GAME_PATH = "current";
    private static final String MISSING_BODY_MESSAGE =
            "No guess was sent. Send a whole number from 1 to 10 as the guess.";

    @Inject
    private GameService games;

    @POST
    @Operation(summary = "Start a game",
            description = "Draws a fresh secret number and resets the guess count, replacing "
                    + "whatever the session was playing.")
    @APIResponse(responseCode = "201", description = "The new game",
            content = @Content(schema = @Schema(implementation = GameStateResponse.class)))
    public Response start(@Context final UriInfo uriInfo) {
        return Response.created(uriInfo.getAbsolutePathBuilder().path(CURRENT_GAME_PATH).build())
                .entity(GameStateResponse.from(games.start()))
                .build();
    }

    @GET
    @Path(CURRENT_GAME_PATH)
    @Operation(summary = "Read the current game",
            description = "The round the session is playing, or the result of the one it just finished.")
    @APIResponse(responseCode = "200", description = "The session's game",
            content = @Content(schema = @Schema(implementation = GameStateResponse.class)))
    @APIResponse(responseCode = "409", description = "The session has no game",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public GameStateResponse current() {
        return GameStateResponse.from(games.current());
    }

    @POST
    @Path(CURRENT_GAME_PATH + "/guesses")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Take a guess",
            description = "Reports whether the guess was too high, too low, or correct. A guess "
                    + "that is not a whole number from 1 to 10 is turned away without counting "
                    + "against the limit.")
    @APIResponse(responseCode = "200", description = "The game after the guess",
            content = @Content(schema = @Schema(implementation = GameStateResponse.class)))
    @APIResponse(responseCode = "400", description = "The guess was not a whole number from 1 to 10",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "The session has no game, or its game is over",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public GameStateResponse guess(
            @NotNull(message = MISSING_BODY_MESSAGE) @Valid final GuessRequest request) {
        return GameStateResponse.from(games.guess(request.value()));
    }
}
