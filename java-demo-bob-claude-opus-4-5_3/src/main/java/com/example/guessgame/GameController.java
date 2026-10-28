package com.example.guessgame;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for game lifecycle: start, guess, and post a score.
 */
@RestController
@RequestMapping("/api/game")
@Tag(name = "Game", description = "Game session management and gameplay")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/start")
    @Operation(
            summary = "Start a new game",
            description = "Creates a new game session and returns its ID.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Session created successfully")
            }
    )
    public StartGameResponse startGame() {
        var sessionId = gameService.startGame();
        return new StartGameResponse(sessionId, Game.getMaxGuesses());
    }

    @PostMapping("/{sessionId}/guess")
    @Operation(
            summary = "Submit a guess",
            description = "Evaluates a single guess and returns the result with updated counts.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Guess evaluated"),
                @ApiResponse(responseCode = "400", description = "Invalid input (number out of range)"),
                @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<GuessResponse> guess(
            @Parameter(description = "Session ID returned by start") @PathVariable String sessionId,
            @Valid @RequestBody GuessRequest request) {

        var result = gameService.submitGuess(sessionId, request.number());
        var game = gameService.getGame(sessionId);

        var message = buildMessage(result, request.number(), game);
        var secretNumber = game.isOver() ? game.getSecretNumber() : null;

        return ResponseEntity.ok(new GuessResponse(
                result,
                game.getGuessCount(),
                game.getGuessesRemaining(),
                secretNumber,
                message
        ));
    }

    @PostMapping("/{sessionId}/score")
    @Operation(
            summary = "Post a winning score",
            description = "Records a winning result on the leaderboard. Only valid for won games. Idempotent: posting again returns 400.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Score recorded"),
                @ApiResponse(responseCode = "400", description = "Invalid name, game not won, or score already posted"),
                @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ScorePostedResponse postScore(
            @Parameter(description = "Session ID of the won game") @PathVariable String sessionId,
            @Valid @RequestBody ScoreRequest request) {

        var entry = gameService.recordScore(sessionId, request.playerName());
        return new ScorePostedResponse(entry.getPlayerName(), entry.getGuessCount(), entry.getRecordedAt());
    }

    private String buildMessage(GuessResult result, int number, Game game) {
        return switch (result) {
            case CORRECT -> String.format("🎉 Correct! You guessed it in %d guess%s!",
                    game.getGuessCount(), game.getGuessCount() == 1 ? "" : "es");
            case TOO_LOW -> String.format("📉 %d is too low. %d guess%s remaining.",
                    number, game.getGuessesRemaining(), game.getGuessesRemaining() == 1 ? "" : "es");
            case TOO_HIGH -> String.format("📈 %d is too high. %d guess%s remaining.",
                    number, game.getGuessesRemaining(), game.getGuessesRemaining() == 1 ? "" : "es");
            case GAME_OVER -> String.format("😞 No more guesses! The number was %d.",
                    game.getSecretNumber());
        };
    }
}
