package com.example.guess.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * JAX-RS application root at `/api`.
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(
    title = "Guess the Number API",
    version = "1.0",
    description = "Server-side API for the Duke Guess the Number game."
))
public class GuessApplication extends Application {}
