package com.example.guessthenumber.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/// A single sentence for the player: either a confirmation or the reason a request was
/// refused.
@Schema(name = "Message", description = "A plain-language sentence for the player")
public record MessageResponse(
        @Schema(description = "What happened and what the player can do next",
                example = "Please pick a whole number between 1 and 10.",
                required = true)
        String message) {
}
