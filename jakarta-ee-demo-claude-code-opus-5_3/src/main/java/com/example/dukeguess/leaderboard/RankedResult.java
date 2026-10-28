package com.example.dukeguess.leaderboard;

/// One row of the leaderboard screen.
public record RankedResult(int rank, String playerName, int guessCount) {

    public static RankedResult of(final int rank, final LeaderboardEntry entry) {
        return new RankedResult(rank, entry.getPlayerName(), entry.getGuessCount());
    }
}
