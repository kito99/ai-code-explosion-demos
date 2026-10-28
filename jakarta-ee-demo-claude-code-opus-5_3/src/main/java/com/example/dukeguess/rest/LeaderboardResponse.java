package com.example.dukeguess.rest;

import com.example.dukeguess.leaderboard.RankedResult;

import java.util.List;

/// The leaderboard screen's data, including the text to show when nothing is posted yet.
public record LeaderboardResponse(List<RankedResult> results, String message) {
}
