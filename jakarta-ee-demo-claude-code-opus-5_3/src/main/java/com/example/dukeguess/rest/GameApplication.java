package com.example.dukeguess.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/// Roots the game's REST API at `/api`.
@ApplicationPath("/api")
public class GameApplication extends Application {
}
