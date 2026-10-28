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
        // revealedNumber is populated only on LOST; the service embeds it in the feedback text,
        // but we also surface it as a typed field for API consumers.
        Integer revealed = game.status() == GameStatus.LOST
                ? extractRevealedNumber(game.lastFeedback())
                : null;
        var response = new GuessResponse(
                game.gameId(),
                game.status(),
                game.lastFeedback(),
                game.guessesUsed(),
                game.guessesRemaining(),
                revealed
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Parses the secret number out of the "No more guesses! The number was N." message.
     * Returns null if parsing fails — the feedback text is the authoritative display.
     */
    private Integer extractRevealedNumber(String feedback) {
        if (feedback == null) return null;
        var marker = "The number was ";
        var idx = feedback.indexOf(marker);
        if (idx < 0) return null;
        try {
            var rest = feedback.substring(idx + marker.length()).replace(".", "").strip();
            return Integer.parseInt(rest);
        } catch (NumberFormatException _) {
            return null;
        }
    }
}
