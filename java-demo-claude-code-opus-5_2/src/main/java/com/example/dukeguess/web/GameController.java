package com.example.dukeguess.web;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.dukeguess.game.GameService;

/// The server-side game actions the browser performs: starting a round, guessing, and
/// posting a win.
@RestController
@RequestMapping("/api/games")
@Tag(name = "Game", description = "Playing a round against Duke")
public class GameController {

    private final GameService gameService;

    public GameController(final GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a round",
            description = "Duke picks a number from 1 to 10 and the guess count resets to zero.")
    @ApiResponse(responseCode = "201", description = "The round is ready to play")
    public GameView startGame() {
        return GameView.of(gameService.startGame());
    }

    @PostMapping("/{gameId}/guesses")
    @Operation(summary = "Submit a guess",
            description = "Scores the guess against Duke's number and reports where the round stands.")
    @ApiResponse(responseCode = "200", description = "The guess was accepted and scored")
    @ApiResponse(responseCode = "400", description = "The guess was not a whole number from 1 to 10",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No such round is in play",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The round is already won or lost",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public GuessView submitGuess(@PathVariable final UUID gameId,
                                 @Valid @RequestBody final GuessRequest request) {
        return GuessView.of(gameService.submitGuess(gameId, request.guess()));
    }

    @PostMapping("/{gameId}/score")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a win to the leaderboard",
            description = "Records the name against the guess count the server scored. "
                    + "A round can produce at most one entry.")
    @ApiResponse(responseCode = "201", description = "The win is on the leaderboard")
    @ApiResponse(responseCode = "400", description = "The name was blank or too long",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No such round is in play",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The round was not won, or its win is already posted",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "The leaderboard database is unreachable",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PostedScoreView postScore(@PathVariable final UUID gameId,
                                     @RequestBody final PostScoreRequest request) {
        return PostedScoreView.of(gameService.postScore(gameId, request.name()));
    }
}
