package com.example.duke.web;

import java.time.Instant;

import com.example.duke.leaderboard.Score;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A score that was just recorded.")
public record ScoreResponse(String name, int guesses, Instant recordedAt) {

    static ScoreResponse from(final Score score) {
        return new ScoreResponse(score.getName(), score.getGuesses(), score.getRecordedAt());
    }
}
