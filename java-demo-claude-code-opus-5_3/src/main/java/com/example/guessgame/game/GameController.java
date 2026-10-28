package com.example.guessgame.game;

import com.example.guessgame.leaderboard.LeaderboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Handles game page and guess submission.
 */
@Controller
@RequestMapping("/")
public class GameController {

    private final GameService gameService;
    private final LeaderboardService leaderboardService;

    public GameController(GameService gameService, LeaderboardService leaderboardService) {
        this.gameService = gameService;
        this.leaderboardService = leaderboardService;
    }

    /** Loads or resumes the game page (FR-1). */
    @GetMapping
    public String index(Model model) {
        var game = gameService.currentGame();
        model.addAttribute("game", game);
        return "game";
    }

    /** Starts a fresh game (FR-12). */
    @PostMapping("/new-game")
    public String newGame() {
        gameService.newGame();
        return "redirect:/";
    }

    /** Submits a guess (FR-5, FR-6, FR-7, FR-8). */
    @PostMapping("/guess")
    public String submitGuess(@RequestParam("guess") String rawGuess, Model model) {
        GameSession game;
        try {
            var number = Integer.parseInt(rawGuess.strip());
            game = gameService.guess(number);
        } catch (NumberFormatException e) {
            // Non-integer input treated as invalid (FR-6)
            game = gameService.guess(-1);
        }
        model.addAttribute("game", game);
        return "game";
    }

    /**
     * Records the win result and redirects to the leaderboard (FR-13, FR-14, FR-15).
     */
    @PostMapping("/post-score")
    public String postScore(
            @RequestParam("playerName") String playerName,
            Model model) {

        // Guard state first: if session has been reset or expired, bail early.
        var currentGame = gameService.currentGame();
        if (currentGame.state() != GameState.WON) {
            return "redirect:/";
        }

        var trimmedName = playerName.strip();
        if (trimmedName.isEmpty() || trimmedName.length() > 30) {
            model.addAttribute("game", currentGame);
            model.addAttribute("nameError", "Name must be between 1 and 30 characters.");
            return "game";
        }

        // Reset before recording to prevent double-submit from re-entering this branch.
        var guessCount = currentGame.guessCount();
        gameService.newGame();
        leaderboardService.recordWin(trimmedName, guessCount);
        return "redirect:/leaderboard";
    }
}
