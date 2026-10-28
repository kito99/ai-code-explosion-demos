package com.example.duke.web;

import com.example.duke.leaderboard.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// Reads the shared leaderboard.
@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard", description = "Best results recorded on this server")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(final LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "Read the leaderboard",
            description = "Returns the top ten results, fewest guesses first, with ties broken by "
                    + "which was recorded first.")
    @ApiResponse(responseCode = "200", description = "The leaderboard as it stands right now")
    public LeaderboardView leaderboard() {
        return new LeaderboardView(leaderboardService.topStandings());
    }
}
