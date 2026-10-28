package com.example.duke.web;

import com.example.duke.game.GuessOutcome;
import com.example.duke.game.GuessResult;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Feedback for an accepted guess plus the updated game state.")
public record GuessResponse(GuessOutcome outcome, GameResponse game) {

    static GuessResponse from(final GuessResult result) {
        return new GuessResponse(result.outcome(), GameResponse.from(result.game()));
    }
}
