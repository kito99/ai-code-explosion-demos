package com.example.duke.web;

import com.example.duke.game.GameError;
import com.example.duke.game.GameException;
import com.example.duke.game.GameService;
import com.example.duke.leaderboard.PlayerName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.example.duke.game.GameRules.MAX_NUMBER;
import static com.example.duke.game.GameRules.MIN_NUMBER;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Game", description = "Play a game of guess the number with Duke.")
public class GameController {

    private final GameService gameService;

    public GameController(final GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a new game", description = "Duke picks a secret number from 1 to 10.")
    @ApiResponse(responseCode = "201", description = "Game started")
    public GameResponse start() {
        return GameResponse.of(gameService.start(), null,
                "Duke is thinking of a number from %d to %d. Can you guess it?".formatted(MIN_NUMBER, MAX_NUMBER));
    }

    @PostMapping("/{gameId}/guesses")
    @Operation(summary = "Make a guess")
    @ApiResponse(responseCode = "200", description = "Guess accepted")
    @ApiResponse(responseCode = "400", description = "Guess is not a whole number from 1 to 10", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown game", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Game already finished", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public GameResponse guess(@PathVariable final UUID gameId, @RequestBody final GuessRequest request) {
        if (request == null || request.guess() == null) {
            throw new GameException(GameError.INVALID_GUESS,
                    "Please enter a whole number from %d to %d.".formatted(MIN_NUMBER, MAX_NUMBER));
        }
        final var result = gameService.guess(gameId, request.guess());
        return GameResponse.of(result.game(), result.outcome(), GuessMessages.describe(result));
    }

    @PostMapping("/{gameId}/score")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a won game to the leaderboard")
    @ApiResponse(responseCode = "201", description = "Score recorded")
    @ApiResponse(responseCode = "400", description = "Invalid name", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown game", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Game not won, or score already posted", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void postScore(@PathVariable final UUID gameId, @RequestBody final PostScoreRequest request) {
        gameService.postScore(gameId, new PlayerName(request == null ? null : request.name()));
    }
}
