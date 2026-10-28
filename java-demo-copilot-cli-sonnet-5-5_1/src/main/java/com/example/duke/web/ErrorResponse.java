package com.example.duke.web;

import com.example.duke.game.GameError;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A refused request.")
public record ErrorResponse(GameError error, @Schema(description = "A friendly message for the player.") String message) {
}
