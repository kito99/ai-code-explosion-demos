package com.example.guessduke.web;

import java.util.List;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// Serves the leaderboard screen with the results as they stand right now,
/// including wins other players posted on this server.
@RestController
@RequestMapping(path = "/api/leaderboard", produces = MediaType.APPLICATION_JSON_VALUE)
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(final LeaderboardService leaderboardService) {
        this.leaderboardService = Objects.requireNonNull(leaderboardService, "leaderboardService");
    }

    /// Returns the ranked leaderboard rows, empty when nothing has been posted.
    @GetMapping
    public List<LeaderboardRowView> bestRounds() {
        return leaderboardService.bestRounds();
    }
}
