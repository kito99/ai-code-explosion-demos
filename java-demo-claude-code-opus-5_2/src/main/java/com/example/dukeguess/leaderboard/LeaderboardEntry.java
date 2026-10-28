package com.example.dukeguess.leaderboard;

import java.time.Instant;

/// One posted win, exactly as it is stored.
public record LeaderboardEntry(PlayerName name, int guesses, Instant recordedAt) {
}
