package com.example.duke.leaderboard;

import java.time.Instant;

/// One row of the leaderboard as players see it.
public record LeaderboardStanding(int rank, String name, int guesses, Instant recordedAt) {
}
