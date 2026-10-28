package com.example.guessthenumber.api;

import com.example.guessthenumber.api.dto.GameStateResponse;
import com.example.guessthenumber.api.dto.GuessRequest;
import com.example.guessthenumber.api.dto.MessageResponse;
import com.example.guessthenumber.game.GameService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static java.util.Objects.requireNonNull;

/// The game actions the screen performs. Each one applies to the game in the caller's own
/// HTTP session.
@Path("game")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = GuessTheNumberApplication.GAME_TAG)
public class GameResource {

    private final GameService gameService;

    @Inject
    public GameResource(final GameService gameService) {
        this.gameService = requireNonNull(gameService, "gameService");
    }

    @POST
    @Operation(summary = "Start a new game",
            description = "Draws a fresh secret number and resets the guess count, replacing any game in progress.")
    @APIResponse(responseCode = "200", description = "The new game",
            content = @Content(schema = @Schema(implementation = GameStateResponse.class)))
    public GameStateResponse startNewGame() {
        final var game = gameService.startNewGame();
        return GameStateResponse.of(game, null, GameMessages.forNewGame(game));
    }

    @GET
    @Operation(summary = "Read the current game",
            description = "Returns the game in the caller's session without changing it.")
    @APIResponse(responseCode = "200", description = "The current game",
            content = @Content(schema = @Schema(implementation = GameStateResponse.class)))
    @APIResponse(responseCode = "404", description = "No game has been started in this session",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    public GameStateResponse currentGame() {
        final var game = gameService.requireGame();
        return GameStateResponse.of(game, null, GameMessages.forCurrentState(game));
    }

    @POST
    @Path("guesses")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Submit a guess",
            description = """
                    Accepted guesses count against the limit and come back with feedback. A guess
                    outside the range is refused and leaves the guess count untouched.
                    """)
    @APIResponse(responseCode = "200", description = "The game after the guess",
            content = @Content(schema = @Schema(implementation = GameStateResponse.class)))
    @APIResponse(responseCode = "400", description = "The guess was missing or outside the range",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @APIResponse(responseCode = "404", description = "No game has been started in this session",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @APIResponse(responseCode = "409", description = "The game is already won or lost",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    public GameStateResponse submitGuess(final GuessRequest request) {
        final var submitted = request == null ? null : request.guess();
        final var result = gameService.submitGuess(submitted);
        return GameStateResponse.of(result.game(), result.outcome(),
                GameMessages.forGuess(submitted, result.outcome(), result.game()));
    }
}
