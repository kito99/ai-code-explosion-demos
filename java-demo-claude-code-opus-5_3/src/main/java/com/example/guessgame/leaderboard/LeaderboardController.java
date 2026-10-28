package com.example.guessgame.leaderboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Serves the leaderboard screen (FR-17 through FR-20).
 */
@Controller
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public String leaderboard(Model model) {
        var entries = leaderboardService.getTopEntries();
        model.addAttribute("entries", entries);
        return "leaderboard";
    }
}
