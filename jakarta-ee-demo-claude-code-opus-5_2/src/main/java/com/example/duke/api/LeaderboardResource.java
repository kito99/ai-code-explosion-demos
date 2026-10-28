package com.example.duke.api;

import com.example.duke.api.dto.ErrorResponse;
import com.example.duke.api.dto.LeaderboardEntryResponse;
import com.example.duke.api.dto.PostedScoreResponse;
import com.example.duke.api.dto.ScoreSubmissionRequest;
import com.example.duke.game.GameService;
import com.example.duke.leaderboard.LeaderboardService;
import com.example.duke.leaderboard.PlayerName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/// The shared leaderboard. Every player on this server posts to the same list, so a read
/// shows whatever stands at that moment, including wins posted by someone else.
@Path("/leaderboard")
@ApplicationScoped
@Produces(APPLICATION_JSON)
@Tag(name = "Leaderboard")
public class LeaderboardResource {

    private static final String MISSING_BODY_MESSAGE =
            "No name was sent. Send the name to post the score under.";

    @Inject
    private LeaderboardService leaderboard;

    @Inject
    private GameService games;

    @GET
    @Operation(summary = "Read the best results",
            description = "The top wins, fewest guesses first, with ties broken by which was "
                    + "recorded first. The list is empty when nothing has been posted yet.")
    @APIResponse(responseCode = "200", description = "The best results, best first",
            content = @Content(schema = @Schema(type = SchemaType.ARRAY,
                    implementation = LeaderboardEntryResponse.class)))
    public List<LeaderboardEntryResponse> bestResults() {
        return LeaderboardEntryResponse.rank(leaderboard.bestResults());
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Post the session's win",
            description = "Records the win the session's game produced, under the name given. "
                    + "The guess count comes from the server's own record of the round.")
    @APIResponse(responseCode = "201", description = "The entry that was recorded",
            content = @Content(schema = @Schema(implementation = PostedScoreResponse.class)))
    @APIResponse(responseCode = "400", description = "The name did not pass the name rules",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409",
            description = "The session has no game, has not won it, or already posted the win",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response post(
            @NotNull(message = MISSING_BODY_MESSAGE) @Valid final ScoreSubmissionRequest request) {
        final var entry = games.postScore(new PlayerName(request.name()));
        return Response.status(Response.Status.CREATED)
                .entity(PostedScoreResponse.from(entry))
                .build();
    }
}
