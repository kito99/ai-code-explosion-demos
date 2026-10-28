package com.example.duke.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A guess for the secret number.")
public record GuessRequest(
        @Schema(description = "Whole number from 1 to 10.", example = "7", requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1", maximum = "10")
        Integer guess) {
}
