package com.example.duke.web;

import com.example.duke.game.GameService;
import com.example.duke.leaderboard.PostedScore;
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

import java.util.UUID;

/// Every server-side game action the front-end performs.
@RestController
@RequestMapping("/api/games")
@Tag(name = "Game", description = "Start a round, guess, and post a win")
public class GameController {

    private final GameService gameService;

    public GameController(final GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a new game",
            description = "Duke picks a number between 1 and 10 and the guess count resets to zero.")
    @ApiResponse(responseCode = "201", description = "A new round is ready")
    public GameView startGame() {
        return GameView.of(gameService.startGame());
    }

    @PostMapping("/{gameId}/guesses")
    @Operation(summary = "Submit a guess",
            description = "Only a whole number between 1 and 10 is accepted, and only an accepted "
                    + "guess counts against the limit of five.")
    @ApiResponse(responseCode = "200", description = "The guess was accepted")
    @ApiResponse(responseCode = "400", description = "The guess was not a whole number between 1 and 10",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No such game",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The game is already over",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public GameView submitGuess(@PathVariable final UUID gameId, @Valid @RequestBody final GuessRequest request) {
        final var result = gameService.submitGuess(gameId, request.guess());
        return GameView.of(result.game(), result.outcome());
    }

    @PostMapping("/{gameId}/score")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a win to the leaderboard",
            description = "Records the name against the guess count the server holds for this win. "
                    + "A game that was lost, or whose score was already posted, is refused.")
    @ApiResponse(responseCode = "201", description = "The win was recorded")
    @ApiResponse(responseCode = "400", description = "The name was blank or longer than 30 characters",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No such game",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The game was not won, or was already posted",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PostedScore postScore(@PathVariable final UUID gameId, @RequestBody final PostScoreRequest request) {
        return gameService.postScore(gameId, request.name());
    }
}
