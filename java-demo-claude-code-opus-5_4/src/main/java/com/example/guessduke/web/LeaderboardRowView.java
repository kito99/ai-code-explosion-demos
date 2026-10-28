package com.example.guessduke.web;

/// One line of the leaderboard screen: where the entry ranks, who posted it, and
/// how many guesses the win took.
public record LeaderboardRowView(int rank, String playerName, int guessCount) {
}
