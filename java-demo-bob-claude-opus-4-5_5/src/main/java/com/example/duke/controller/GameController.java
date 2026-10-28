package com.example.duke.controller;

import com.example.duke.model.GameState;
import com.example.duke.model.GameStatus;
import com.example.duke.service.GameService;
import com.example.duke.service.LeaderboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the game UI: start, guessing, and score posting.
 */
@Controller
public class GameController {

    private static final String SESSION_GAME = "game";

    private final GameService gameService;
    private final LeaderboardService leaderboardService;

    public GameController(GameService gameService, LeaderboardService leaderboardService) {
        this.gameService = gameService;
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        var game = (GameState) session.getAttribute(SESSION_GAME);
        if (game == null) {
            game = gameService.newGame();
            session.setAttribute(SESSION_GAME, game);
        }
        model.addAttribute("game", game);
        return "index";
    }

    @PostMapping("/guess")
    public String guess(
            @RequestParam("guess") String guessInput,
            HttpSession session,
            RedirectAttributes redirectAttrs
    ) {
        var game = (GameState) session.getAttribute(SESSION_GAME);
        if (game == null) {
            game = gameService.newGame();
        }

        int guessValue;
        try {
            guessValue = Integer.parseInt(guessInput.trim());
        } catch (NumberFormatException _) {
            guessValue = -1; // will fail validation inside GameService
        }

        var updated = gameService.guess(game, guessValue);
        session.setAttribute(SESSION_GAME, updated);
        return "redirect:/";
    }

    @PostMapping("/new-game")
    public String newGame(HttpSession session) {
        session.setAttribute(SESSION_GAME, gameService.newGame());
        return "redirect:/";
    }

    @PostMapping("/post-score")
    public String postScore(
            @RequestParam("playerName") String playerName,
            HttpSession session,
            RedirectAttributes redirectAttrs
    ) {
        var game = (GameState) session.getAttribute(SESSION_GAME);
        if (game == null || game.status() != GameStatus.WON) {
            return "redirect:/";
        }

        try {
            leaderboardService.record(playerName, game.guessesUsed());
            session.setAttribute(SESSION_GAME, gameService.newGame());
            return "redirect:/leaderboard";
        } catch (IllegalArgumentException ex) {
            redirectAttrs.addFlashAttribute("nameError", ex.getMessage());
            return "redirect:/";
        }
    }
}
