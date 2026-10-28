package com.example.guess.rest;

import com.example.guess.entity.LeaderboardEntry;
import com.example.guess.game.GameSessionBean;
import com.example.guess.leaderboard.LeaderboardService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST resource for leaderboard operations.
 *
 * GET  /api/leaderboard        — fetch top-10 (FR-17, FR-18, FR-20)
 * POST /api/leaderboard        — post a win score (FR-13, FR-14, FR-15)
 */
@Path("/leaderboard")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LeaderboardResource {

    @Inject
    LeaderboardService leaderboardService;

    @Inject
    GameSessionBean sessionBean;

    /** FR-17, FR-18, FR-20: return top-10. */
    @GET
    public Response getLeaderboard() {
        List<LeaderboardEntry> entries = leaderboardService.getTop10();
        List<Map<String, Object>> view = buildView(entries);
        return Response.ok(view).build();
    }

    /**
     * FR-13, FR-14, FR-15: post a win. The current session must be in the won state.
     * Jakarta Validation fires on the {@link ScoreRequest} parameter.
     */
    @POST
    public Response postScore(@Valid ScoreRequest req) {
        if (!sessionBean.current().isWon()) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("message", "No active win to record."))
                           .build();
        }
        int guessesTaken = sessionBean.current().getGuessesMade();
        LeaderboardEntry entry = leaderboardService.recordWin(req.getName().strip(), guessesTaken);
        return Response.status(Response.Status.CREATED)
                       .entity(toMap(entry))
                       .build();
    }

    // ---- helpers ----

    private List<Map<String, Object>> buildView(List<LeaderboardEntry> entries) {
        int[] rank = {1};
        return entries.stream()
                      .map(e -> {
                          Map<String, Object> m = toMap(e);
                          m.put("rank", rank[0]++);
                          return m;
                      })
                      .toList();
    }

    @SuppressWarnings("unchecked")
    private java.util.HashMap<String, Object> toMap(LeaderboardEntry e) {
        java.util.HashMap<String, Object> m = new java.util.HashMap<>();
        m.put("id",           e.getId());
        m.put("playerName",   e.getPlayerName());
        m.put("guessesTaken", e.getGuessesTaken());
        m.put("recordedAt",   e.getRecordedAt().toString());
        return m;
    }
}
