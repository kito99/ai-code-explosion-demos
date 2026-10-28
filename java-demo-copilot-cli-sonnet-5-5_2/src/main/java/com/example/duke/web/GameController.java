package com.example.duke.web;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.duke.game.GameService;
import com.example.duke.game.InvalidGuessException;
import com.example.duke.leaderboard.LeaderboardEntry;
import com.example.duke.leaderboard.LeaderboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Game", description = "Play a round and post winning scores.")
public class GameController {

    private static final String ERROR_DESCRIPTION_BAD_REQUEST = "The request was not acceptable; the message says why.";

    private final GameService gameService;
    private final LeaderboardService leaderboardService;

    public GameController(final GameService gameService, final LeaderboardService leaderboardService) {
        this.gameService = gameService;
        this.leaderboardService = leaderboardService;
    }

    @Operation(summary = "Start a new game with a fresh secret number.")
    @ApiResponse(responseCode = "201", description = "Game started.")
    @PostMapping("/games")
    public ResponseEntity<GameResponse> startGame() {
        return ResponseEntity.status(CREATED).body(GameResponse.from(gameService.startGame()));
    }

    @Operation(summary = "Submit a guess for the secret number.")
    @ApiResponse(responseCode = "200", description = "Guess accepted; shows whether it was too low, too high or correct.")
    @ApiResponse(responseCode = "400", description = ERROR_DESCRIPTION_BAD_REQUEST,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown game.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The game is already over.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/games/{gameId}/guesses")
    public GuessResponse guess(@PathVariable final String gameId, @RequestBody final GuessRequest request) {
        if (request.guess() == null) {
            throw new InvalidGuessException();
        }
        return GuessResponse.from(gameService.guess(gameId, request.guess()));
    }

    @Operation(summary = "Post the score of a won game to the leaderboard.")
    @ApiResponse(responseCode = "201", description = "Score recorded.")
    @ApiResponse(responseCode = "400", description = ERROR_DESCRIPTION_BAD_REQUEST,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown game.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The game was not won or its score was already posted.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/games/{gameId}/score")
    public ResponseEntity<ScoreResponse> postScore(@PathVariable final String gameId,
            @RequestBody final ScoreRequest request) {
        final var score = leaderboardService.postScore(gameId, request.name());
        return ResponseEntity.status(CREATED).body(ScoreResponse.from(score));
    }

    @Tag(name = "Leaderboard", description = "Best results across all players.")
    @Operation(summary = "List the top 10 wins, fewest guesses first, earliest first on ties.")
    @ApiResponse(responseCode = "200", description = "Ranked entries; empty when no scores were posted.")
    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard() {
        return leaderboardService.topEntries();
    }
}
