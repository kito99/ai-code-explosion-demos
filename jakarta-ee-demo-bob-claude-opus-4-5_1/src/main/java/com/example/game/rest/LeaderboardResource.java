package com.example.game.rest;

import com.example.game.model.LeaderboardEntry;
import com.example.game.service.GameService;
import com.example.game.service.LeaderboardService;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST resource for the leaderboard.
 *
 * - `GET  /api/leaderboard`         — top-10 entries
 * - `POST /api/leaderboard`         — post a winning score
 */
@Path("/leaderboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LeaderboardResource {

    @Inject
    LeaderboardService leaderboardService;

    @GET
    public Response getLeaderboard() {
        var entries = leaderboardService.getTopEntries();
        var dtos = new java.util.ArrayList<LeaderboardEntryDto>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            dtos.add(new LeaderboardEntryDto(i + 1, entry.getPlayerName(), entry.getGuessCount()));
        }
        return Response.ok(dtos).build();
    }

    @POST
    public Response postScore(ScoreRequest request) {
        if (request == null) {
            return badRequest("Name is required.");
        }

        var name = request.playerName() == null ? "" : request.playerName().strip();
        if (name.isEmpty()) {
            return badRequest("Please enter a name before posting your score.");
        }
        if (name.length() > 30) {
            return badRequest("Name must be 30 characters or fewer.");
        }

        leaderboardService.postScore(name, request.guessCount());
        return Response.status(Response.Status.CREATED).build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .build();
    }

    // --- Nested DTOs ---

    public record ScoreRequest(String playerName, int guessCount) {}

    public record LeaderboardEntryDto(int rank, String playerName, int guessCount) {}

    public record ErrorResponse(String message) {}
}
