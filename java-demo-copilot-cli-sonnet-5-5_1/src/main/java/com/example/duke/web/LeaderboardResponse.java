package com.example.duke.web;

import com.example.duke.leaderboard.LeaderboardStanding;

import java.util.List;

public record LeaderboardResponse(List<LeaderboardStanding> entries) {
}
