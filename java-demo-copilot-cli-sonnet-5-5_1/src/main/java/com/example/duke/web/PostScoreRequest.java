package com.example.duke.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A request to post a won game to the leaderboard.")
public record PostScoreRequest(@Schema(description = "Player name, 1 to 30 characters after trimming.", example = "Duke",
        maxLength = 30) String name) {
}
