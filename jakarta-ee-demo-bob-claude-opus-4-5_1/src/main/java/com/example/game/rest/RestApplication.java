package com.example.game.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** JAX-RS activation; all REST resources live under /api. */
@ApplicationPath("/api")
public class RestApplication extends Application {}
