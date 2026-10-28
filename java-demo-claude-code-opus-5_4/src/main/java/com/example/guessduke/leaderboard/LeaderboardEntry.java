package com.example.guessduke.leaderboard;

import java.time.Instant;

/// One recorded win: who posted it, how many guesses it took, and when it happened.
public record LeaderboardEntry(long id, String playerName, int guessCount, Instant recordedAt) {
}
