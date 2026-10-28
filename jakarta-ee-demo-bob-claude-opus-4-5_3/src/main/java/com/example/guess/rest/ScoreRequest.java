package com.example.guess.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Incoming JSON payload for POST /api/leaderboard. FR-13, FR-14. */
public class ScoreRequest {

    @NotBlank(message = "Name must have at least one visible character")
    @Size(max = 30, message = "Name must be 30 characters or fewer")
    private String name;

    public String getName()           { return name; }
    public void   setName(String n)   { this.name = n; }
}
