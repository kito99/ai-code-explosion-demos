package com.example.duke.web;

import com.example.duke.leaderboard.LeaderboardStanding;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/// The leaderboard as it stands right now.
///
/// @param entries best results, ranked from first place down, empty when nothing has been posted
@Schema(name = "Leaderboard", description = "Best results recorded on this server")
public record LeaderboardView(List<LeaderboardStanding> entries) {
}
