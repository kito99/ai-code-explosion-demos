package com.example.duke.leaderboard;

import java.time.Instant;

public record LeaderboardEntry(int rank, String name, int guesses, Instant recordedAt) {
}
