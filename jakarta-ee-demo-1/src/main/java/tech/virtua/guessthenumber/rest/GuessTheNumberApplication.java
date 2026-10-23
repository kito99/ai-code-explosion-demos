package tech.virtua.guessthenumber.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Puts the game's REST endpoints under /api. */
@ApplicationPath("/api")
public class GuessTheNumberApplication extends Application {
}
