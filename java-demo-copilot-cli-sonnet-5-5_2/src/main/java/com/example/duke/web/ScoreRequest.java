package com.example.duke.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The name to post a winning score under.")
public record ScoreRequest(
        @Schema(description = "1 to 30 visible characters after trimming whitespace.", example = "Duke",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 30)
        String name) {
}
