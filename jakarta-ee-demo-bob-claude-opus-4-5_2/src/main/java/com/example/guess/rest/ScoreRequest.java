package com.example.guess.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request body for posting a leaderboard score. Jakarta Validation enforces the
 * name rules described in FR-14 at the REST boundary.
 *
 * Validation runs on the raw (un-trimmed) value; the resource method strips
 * whitespace before persisting. A name padded with trailing spaces that pushes
 * the raw length past 30 is rejected, which is the stricter-but-safer choice.
 */
@Schema(description = "A score posting request")
public record ScoreRequest(
    @Schema(description = "Player name (1–30 visible characters, trimmed)", required = true)
    @NotBlank(message = "Name must contain at least one visible character.")
    @Size(max = 30, message = "Name must be 30 characters or fewer (after trimming).")
    String playerName
) {}
