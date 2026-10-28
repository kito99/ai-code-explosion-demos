package com.example.guess.rest;

import com.example.guess.game.GameSessionStore;
import com.example.guess.leaderboard.LeaderboardService;
import com.example.guess.model.LeaderboardEntryView;
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

import java.util.List;

/**
 * REST endpoints for leaderboard operations.
 */
@Path("/leaderboard")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Leaderboard", description = "Leaderboard operations")
public class LeaderboardResource {

    @Inject
    private LeaderboardService leaderboardService;

    @Inject
    private GameSessionStore sessionStore;

    @GET
    @Operation(summary = "Get top 10 leaderboard entries",
               description = "Returns up to 10 entries ordered by fewest guesses, ties broken by earliest recorded.")
    @APIResponse(responseCode = "200", description = "Leaderboard entries",
                 content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = LeaderboardEntryView.class)))
    public List<LeaderboardEntryView> getLeaderboard() {
        return leaderboardService.fetchTop();
    }

    @POST
    @Path("/{sessionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Post a winning score",
               description = "Records the player's name and winning guess count for the given session. " +
                             "The session must be in a won state. " +
                             "Jakarta Validation enforces name length and non-blank rules (FR-14).")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Score recorded"),
        @APIResponse(responseCode = "400", description = "Name validation failed"),
        @APIResponse(responseCode = "404", description = "Session not found"),
        @APIResponse(responseCode = "409", description = "Session is not in a won state"),
        @APIResponse(responseCode = "422", description = "Invalid name — blank or exceeds 30 characters")
    })
    public Response postScore(
            @Parameter(description = "Game session ID", required = true)
            @PathParam("sessionId") String sessionId,
            @RequestBody(description = "Player name", required = true,
                         content = @Content(schema = @Schema(implementation = ScoreRequest.class)))
            @Valid ScoreRequest request) {

        var session = sessionStore.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Session not found."))
                    .build();
        }
        if (!session.isWon()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorMessage("Session has not been won yet."))
                    .build();
        }

        var name = request.playerName().strip();
        leaderboardService.record(name, session.guessCount());
        sessionStore.removeSession(sessionId);
        return Response.status(Response.Status.CREATED).build();
    }
}
