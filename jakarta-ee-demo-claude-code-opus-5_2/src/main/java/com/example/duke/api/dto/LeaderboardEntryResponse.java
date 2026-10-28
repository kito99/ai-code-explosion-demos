package com.example.duke.api.dto;

import com.example.duke.leaderboard.LeaderboardEntry;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

/// One row of the leaderboard. The rank comes from the row's place in the list that was
/// read, so it is only meaningful alongside the rest of that list.
@Schema(name = "LeaderboardEntry", description = "One posted win")
public record LeaderboardEntryResponse(

        @Schema(description = "Place in the list, starting at 1")
        int rank,

        @Schema(description = "The name the player posted the win under")
        String name,

        @Schema(description = "How many guesses the win took")
        int guessCount,

        @Schema(description = "When the win was posted")
        Instant recordedAt) {

    /// Ranks `entries` in the order they were read, which is already best-first.
    public static List<LeaderboardEntryResponse> rank(final List<LeaderboardEntry> entries) {
        return IntStream.range(0, entries.size())
                .mapToObj(index -> of(index + 1, entries.get(index)))
                .toList();
    }

    private static LeaderboardEntryResponse of(final int rank, final LeaderboardEntry entry) {
        return new LeaderboardEntryResponse(rank, entry.playerName(), entry.guessCount(), entry.recordedAt());
    }
}
