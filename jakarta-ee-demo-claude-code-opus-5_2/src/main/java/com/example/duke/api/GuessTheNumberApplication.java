package com.example.duke.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/// Roots every server-side game action under `/api`. MicroProfile OpenAPI builds the
/// definition from these classes, so it is generated from the implementation rather than
/// maintained beside it.
@ApplicationPath("/api")
@OpenAPIDefinition(
        info = @Info(
                title = "Guess the Number with Duke",
                version = "1.0.0",
                description = """
                        Guess a whole number from 1 to 10 against Duke. A game belongs to the \
                        caller's HTTP session, so the session cookie is what ties a guess to a \
                        game. The server keeps the secret number and the guess count, which is \
                        why a caller cannot read the answer early or post a better score than \
                        it played."""),
        tags = {
                @Tag(name = "Game", description = "Playing a round against Duke"),
                @Tag(name = "Leaderboard", description = "Posting and reading wins")
        })
public class GuessTheNumberApplication extends Application {
}
