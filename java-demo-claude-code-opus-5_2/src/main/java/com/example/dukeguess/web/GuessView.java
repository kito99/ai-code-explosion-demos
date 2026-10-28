package com.example.dukeguess.web;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.dukeguess.game.GuessOutcome;
import com.example.dukeguess.game.GuessVerdict;

/// How one guess landed, together with the state of the round after it.
@Schema(name = "GuessResult", description = "The verdict on a guess and the round it belongs to")
public record GuessView(GuessVerdict verdict, GameView game) {

    public static GuessView of(final GuessOutcome outcome) {
        return new GuessView(outcome.verdict(), GameView.of(outcome.game()));
    }
}
