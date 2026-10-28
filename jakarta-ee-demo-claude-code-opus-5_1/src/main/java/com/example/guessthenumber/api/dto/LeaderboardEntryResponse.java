package com.example.guessthenumber.api.dto;

import com.example.guessthenumber.leaderboard.LeaderboardEntry;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/// One row of the leaderboard.
@Schema(name = "LeaderboardEntry", description = "A posted win and where it ranks")
public record LeaderboardEntryResponse(
        @Schema(description = "Position in the leaderboard, starting at 1", example = "1", required = true)
        int rank,

        @Schema(description = "The name the winner posted under", example = "Ada", required = true)
        String playerName,

        @Schema(description = "How many guesses the win took", example = "2", required = true)
        int guessCount) {

    public static LeaderboardEntryResponse of(final int rank, final LeaderboardEntry entry) {
        return new LeaderboardEntryResponse(rank, entry.playerName(), entry.guessCount());
    }
}
