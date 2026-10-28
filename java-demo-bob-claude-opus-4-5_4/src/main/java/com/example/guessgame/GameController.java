package com.example.guessgame;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GameController {

    private final LeaderboardService leaderboardService;

    public GameController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /** Load (or create) the game session from the HTTP session. */
    private GameSession getOrCreate(HttpSession session) {
        GameSession game = (GameSession) session.getAttribute("game");
        if (game == null) {
            game = new GameSession();
            session.setAttribute("game", game);
        }
        return game;
    }

    /** GET / — show the game page. */
    @GetMapping("/")
    public String gamePage(HttpSession session, Model model) {
        GameSession game = getOrCreate(session);
        populateModel(model, game, null, null);
        return "game";
    }

    /** POST /guess — submit a guess. */
    @PostMapping("/guess")
    public String guess(
            @RequestParam("guess") String guessParam,
            HttpSession session,
            Model model) {

        GameSession game = getOrCreate(session);

        if (game.getStatus() != GameSession.Status.ACTIVE) {
            populateModel(model, game, null, null);
            return "game";
        }

        // Validate input
        int guess;
        try {
            guess = Integer.parseInt(guessParam.trim());
        } catch (NumberFormatException e) {
            populateModel(model, game, "Please enter a whole number between 1 and 10.", null);
            return "game";
        }

        if (guess < 1 || guess > 10) {
            populateModel(model, game, "Please enter a number between 1 and 10.", null);
            return "game";
        }

        String feedback = game.processGuess(guess);
        populateModel(model, game, feedback, null);
        return "game";
    }

    /** POST /new-game — start a fresh game. */
    @PostMapping("/new-game")
    public String newGame(HttpSession session) {
        GameSession game = getOrCreate(session);
        game.reset();
        return "redirect:/";
    }

    /** POST /submit-score — record a win on the leaderboard. */
    @PostMapping("/submit-score")
    public String submitScore(
            @RequestParam("playerName") String playerName,
            HttpSession session,
            Model model) {

        GameSession game = getOrCreate(session);

        if (game.getStatus() != GameSession.Status.WON) {
            return "redirect:/";
        }

        String trimmed = playerName == null ? "" : playerName.strip();
        if (trimmed.isEmpty()) {
            populateModel(model, game, null, "Please enter your name to post your score.");
            return "game";
        }
        if (trimmed.length() > 30) {
            populateModel(model, game, null, "Name must be 30 characters or fewer.");
            return "game";
        }

        leaderboardService.recordWin(trimmed, game.getGuessCount());
        // Reset game so it is ready for the next round
        game.reset();
        session.setAttribute("game", game);
        return "redirect:/leaderboard";
    }

    /** GET /leaderboard — show the leaderboard. */
    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("entries", leaderboardService.getTopEntries());
        return "leaderboard";
    }

    // -----------------------------------------------------------------------
    private void populateModel(Model model, GameSession game, String validationMessage, String nameError) {
        model.addAttribute("guessCount", game.getGuessCount());
        model.addAttribute("guessesRemaining", game.getGuessesRemaining());
        model.addAttribute("status", game.getStatus().name());
        model.addAttribute("feedback", game.getLastFeedback());
        model.addAttribute("validationMessage", validationMessage);
        model.addAttribute("nameError", nameError);
        model.addAttribute("maxGuesses", game.getMaxGuesses());
        model.addAttribute("messageClass", resolveMessageClass(game, validationMessage));
        String displayMsg = validationMessage != null ? validationMessage
                          : (game.getLastFeedback() != null ? game.getLastFeedback() : "Make your first guess!");
        model.addAttribute("displayMessage", displayMsg);
        if (game.getStatus() == GameSession.Status.LOST) {
            model.addAttribute("secretNumber", game.getSecretNumber());
        }
    }

    private String resolveMessageClass(GameSession game, String validationMessage) {
        if (validationMessage != null) return "error";
        if (game.getStatus() == GameSession.Status.WON) return "won";
        if (game.getStatus() == GameSession.Status.LOST) return "lost";
        String feedback = game.getLastFeedback();
        if (feedback != null && feedback.toLowerCase().contains("low")) return "too-low";
        if (feedback != null && feedback.toLowerCase().contains("high")) return "too-high";
        return "";
    }
}
