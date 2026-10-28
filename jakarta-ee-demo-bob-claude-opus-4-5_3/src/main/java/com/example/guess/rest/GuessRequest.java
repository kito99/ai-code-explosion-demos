package com.example.guess.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Incoming JSON payload for POST /api/game/guess. FR-5, FR-6. */
public class GuessRequest {

    @NotNull(message = "Guess is required")
    @Min(value = 1,  message = "Guess must be between 1 and 10")
    @Max(value = 10, message = "Guess must be between 1 and 10")
    private Integer guess;

    public Integer getGuess()             { return guess; }
    public void    setGuess(Integer g)    { this.guess = g; }
}
