package com.example.guessthenumber.api;

import com.example.guessthenumber.api.dto.LeaderboardEntryResponse;
import com.example.guessthenumber.api.dto.MessageResponse;
import com.example.guessthenumber.api.dto.ScoreSubmissionRequest;
import com.example.guessthenumber.leaderboard.LeaderboardEntry;
import com.example.guessthenumber.leaderboard.LeaderboardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/// The shared leaderboard: read the best wins, or post the one in the caller's session.
@Path("leaderboard")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = GuessTheNumberApplication.LEADERBOARD_TAG)
public class LeaderboardResource {

    private final LeaderboardService leaderboardService;

    @Inject
    public LeaderboardResource(final LeaderboardService leaderboardService) {
        this.leaderboardService = requireNonNull(leaderboardService, "leaderboardService");
    }

    @GET
    @Operation(summary = "Read the leaderboard",
            description = """
                    The best wins recorded by every player on this server, fewest guesses first,
                    with ties broken by which was recorded first. Returns an empty list when
                    nothing has been posted yet.
                    """)
    @APIResponse(responseCode = "200", description = "The best wins, ranked",
            content = @Content(schema = @Schema(implementation = LeaderboardEntryResponse.class,
                    type = SchemaType.ARRAY)))
    public List<LeaderboardEntryResponse> bestEntries() {
        final List<LeaderboardEntry> entries = leaderboardService.bestEntries();
        return IntStream.range(0, entries.size())
                .mapToObj(index -> LeaderboardEntryResponse.of(index + 1, entries.get(index)))
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Post the session's win",
            description = """
                    Records the caller's won game under the given name. The guess count comes from
                    the game itself, and a win can only be posted once.
                    """)
    @APIResponse(responseCode = "201", description = "The win was recorded",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @APIResponse(responseCode = "400", description = "The name was blank or too long",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @APIResponse(responseCode = "404", description = "No game has been started in this session",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @APIResponse(responseCode = "409", description = "There is no unposted win in this session",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    public Response postScore(final ScoreSubmissionRequest request) {
        final var submittedName = request == null ? null : request.playerName();
        final var entry = leaderboardService.recordWin(submittedName);
        final var message = new MessageResponse("Nice one, %s! Your %d-guess win is on the leaderboard."
                .formatted(entry.playerName(), entry.guessCount()));
        return Response.status(Response.Status.CREATED).entity(message).build();
    }
}
