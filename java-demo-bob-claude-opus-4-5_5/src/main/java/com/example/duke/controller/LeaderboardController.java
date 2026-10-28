package com.example.duke.controller;

import com.example.duke.service.LeaderboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the leaderboard screen.
 */
@Controller
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        var entries = leaderboardService.getTopEntries();
        model.addAttribute("entries", entries);
        model.addAttribute("empty", entries.isEmpty());
        return "leaderboard";
    }
}
