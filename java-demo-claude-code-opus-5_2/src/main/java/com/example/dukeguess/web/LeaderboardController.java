package com.example.dukeguess.web;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dukeguess.leaderboard.LeaderboardService;

/// Reads the shared leaderboard.
@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard", description = "The best results posted on this server")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(final LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "Read the standings",
            description = "The top results as they stand right now, fewest guesses first, "
                    + "ties going to whoever posted first. Empty when nothing has been posted.")
    @ApiResponse(responseCode = "200", description = "The standings, at most ten rows")
    @ApiResponse(responseCode = "503", description = "The leaderboard database is unreachable",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public List<StandingView> standings() {
        return leaderboardService.standings().stream()
                .map(StandingView::of)
                .toList();
    }
}
