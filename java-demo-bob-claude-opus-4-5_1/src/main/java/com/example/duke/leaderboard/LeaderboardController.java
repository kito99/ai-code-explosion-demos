package com.example.duke.leaderboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for reading the leaderboard and posting scores.
 */
@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard", description = "Read and post leaderboard scores")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "Get top 10 leaderboard entries",
               description = "Returns up to 10 entries ordered by fewest guesses, then earliest recorded.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leaderboard returned (may be empty)"),
    })
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard() {
        return ResponseEntity.ok(leaderboardService.listTop10());
    }

    @PostMapping
    @Operation(summary = "Post a score to the leaderboard",
               description = "Records a winning game result. The game must be in the WON state.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Score recorded"),
            @ApiResponse(responseCode = "400", description = "Validation failure or game not won"),
            @ApiResponse(responseCode = "404", description = "Game not found"),
    })
    public ResponseEntity<LeaderboardEntryDto> postScore(@Valid @RequestBody PostScoreRequest request) {
        var entry = leaderboardService.postScore(request.gameId(), request.playerName());
        return ResponseEntity.status(201).body(entry);
    }
}
