package com.example.dukeguess.web;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.dukeguess.leaderboard.LeaderboardStanding;

/// One row of the leaderboard.
@Schema(name = "Standing", description = "A ranked result on the leaderboard")
public record StandingView(int rank, String name, int guesses) {

    public static StandingView of(final LeaderboardStanding standing) {
        return new StandingView(standing.rank(), standing.name().value(), standing.guesses());
    }
}
