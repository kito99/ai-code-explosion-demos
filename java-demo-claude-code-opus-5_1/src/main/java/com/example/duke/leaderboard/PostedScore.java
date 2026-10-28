package com.example.duke.leaderboard;

import java.time.Instant;

/// The result that was just added to the leaderboard.
public record PostedScore(String name, int guesses, Instant recordedAt) {
}
