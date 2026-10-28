package com.example.guessthenumber.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/// Roots the HTTP API at `/api` and supplies the metadata that heads the OpenAPI document.
@ApplicationPath("/api")
@OpenAPIDefinition(
        info = @Info(
                title = "Guess the Number with Duke",
                version = "1.0.0",
                description = """
                        Every server-side action the game screen performs. A game belongs to the
                        caller's HTTP session: start one, guess until you win or run out, then post
                        the win to the shared leaderboard.
                        """,
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")),
        tags = {
                @Tag(name = GuessTheNumberApplication.GAME_TAG,
                        description = "Start a game, guess, and read its state"),
                @Tag(name = GuessTheNumberApplication.LEADERBOARD_TAG,
                        description = "Post a win and read the best results")
        })
public class GuessTheNumberApplication extends Application {

    public static final String GAME_TAG = "Game";
    public static final String LEADERBOARD_TAG = "Leaderboard";
}
