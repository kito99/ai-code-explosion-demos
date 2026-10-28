package com.example.dukeguess.rest;

import jakarta.validation.constraints.NotNull;

/// The name a winning player wants on the leaderboard.
public record ScoreSubmission(@NotNull(message = "Please enter a name so we can post your score.") String playerName) {
}
