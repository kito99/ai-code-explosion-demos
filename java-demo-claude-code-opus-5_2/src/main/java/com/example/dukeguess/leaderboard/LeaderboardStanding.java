package com.example.dukeguess.leaderboard;

/// A posted win with the place it holds on the leaderboard.
public record LeaderboardStanding(int rank, PlayerName name, int guesses) {
}
