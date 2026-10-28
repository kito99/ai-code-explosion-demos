package com.example.dukeguess.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.dukeguess.leaderboard.LeaderboardEntry;

/// The leaderboard entry a win produced.
@Schema(name = "PostedScore", description = "A win as it was written to the leaderboard")
public record PostedScoreView(String name, int guesses, Instant recordedAt) {

    public static PostedScoreView of(final LeaderboardEntry entry) {
        return new PostedScoreView(entry.name().value(), entry.guesses(), entry.recordedAt());
    }
}
