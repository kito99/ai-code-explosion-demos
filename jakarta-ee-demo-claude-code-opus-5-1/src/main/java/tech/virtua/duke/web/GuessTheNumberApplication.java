package tech.virtua.duke.web;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/// Puts the game's endpoints under `/api`, leaving the rest of the context root to the
/// front end.
@ApplicationPath("/api")
public class GuessTheNumberApplication extends Application {
}
