package com.example.guessgame;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * REST endpoint for the leaderboard.
 */
@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard", description = "Top-10 winning results")
public class LeaderboardController {

    private final GameService gameService;

    public LeaderboardController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    @Operation(
            summary = "Get the leaderboard",
            description = "Returns the top 10 results ordered by fewest guesses, ties broken by earliest recorded time.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Leaderboard retrieved")
            }
    )
    public List<LeaderboardEntryResponse> getLeaderboard() {
        var entries = gameService.getLeaderboard();
        var ranked = new ArrayList<LeaderboardEntryResponse>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            ranked.add(new LeaderboardEntryResponse(
                    i + 1,
                    entry.getPlayerName(),
                    entry.getGuessCount(),
                    entry.getRecordedAt()
            ));
        }
        return ranked;
    }
}
