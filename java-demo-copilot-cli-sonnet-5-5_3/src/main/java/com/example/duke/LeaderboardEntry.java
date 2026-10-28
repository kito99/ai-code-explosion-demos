package com.example.duke;

import java.time.Instant;

/** A posted winning result. */
public record LeaderboardEntry(long id, String playerName, int guesses, Instant recordedAt) {
}
