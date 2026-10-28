package com.example.guess.rest;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request body for posting a leaderboard score.
 */
@Schema(description = "A score posting request")
public record ScoreRequest(
    @Schema(description = "Player name (1–30 visible characters)", required = true)
    String playerName
) {}
