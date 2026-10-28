package com.example.duke.game;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for starting games and submitting guesses.
 */
@RestController
@RequestMapping("/api/game")
@Tag(name = "Game", description = "Game lifecycle and guess submission")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start a new game", description = "Picks a secret number and returns a game ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Game started"),
    })
    public ResponseEntity<GameStartResponse> startGame() {
        var game = gameService.startGame();
        var response = new GameStartResponse(
                game.gameId(),
                game.guessesRemaining(),
                "I'm thinking of a number between %d and %d. You have %d guesses!"
                        .formatted(Game.MIN_NUMBER, Game.MAX_NUMBER, Game.MAX_GUESSES)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/guess")
    @Operation(summary = "Submit a guess", description = "Evaluates the guess and returns feedback.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guess processed"),
            @ApiResponse(responseCode = "400", description = "Invalid input or game already ended"),
            @ApiResponse(responseCode = "404", description = "Game not found"),
    })
    public ResponseEntity<GuessResponse> guess(@Valid @RequestBody GuessRequest request) {
        var game = gameService.guess(request.gameId(), request.guess());
        var response = new GuessResponse(
                game.gameId(),
                game.status(),
                game.lastFeedback(),
                game.guessesUsed(),
                game.guessesRemaining(),
                game.revealedNumber()
        );
        return ResponseEntity.ok(response);
    }
}
