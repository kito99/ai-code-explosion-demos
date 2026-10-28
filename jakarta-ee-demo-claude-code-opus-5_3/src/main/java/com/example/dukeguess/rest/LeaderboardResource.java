package com.example.dukeguess.rest;

import com.example.dukeguess.game.GameSession;
import com.example.dukeguess.leaderboard.LeaderboardService;
import com.example.dukeguess.leaderboard.PlayerName;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;

/// Reads the leaderboard and posts the current session's win to it.
@Path("/leaderboard")
@RequestScoped
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class LeaderboardResource {

    private static final String EMPTY_LEADERBOARD_MESSAGE = "No scores have been posted so far. Win a game to be first!";

    @Inject
    private LeaderboardService leaderboardService;

    @Inject
    private GameSession gameSession;

    @GET
    public LeaderboardResponse bestResults() {
        final var results = leaderboardService.bestResults();
        return new LeaderboardResponse(results, results.isEmpty() ? EMPTY_LEADERBOARD_MESSAGE : null);
    }

    @POST
    public Response postScore(@Valid final ScoreSubmission submission) {
        final var guessCount = gameSession.postableWin()
                .orElseThrow(() -> new NoPostableWinException(
                        "There is no win to post right now. Win a game and your score can go up."));
        final var playerName = PlayerName.of(submission == null ? null : submission.playerName());

        if (!gameSession.claimWinForPosting()) {
            throw new NoPostableWinException("That win is already on the leaderboard.");
        }
        try {
            leaderboardService.postScore(playerName, guessCount);
        } catch (final RuntimeException exception) {
            gameSession.releaseWinForPosting();
            throw exception;
        }

        return Response.status(Response.Status.CREATED)
                .entity(new PostedScoreResponse(
                        playerName.value(),
                        guessCount,
                        format("Thanks, %s! Your score is on the leaderboard.", playerName.value())))
                .build();
    }
}
