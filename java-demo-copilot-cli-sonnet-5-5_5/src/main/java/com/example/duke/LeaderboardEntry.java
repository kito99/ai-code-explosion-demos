package com.example.duke;

import java.time.Instant;

public record LeaderboardEntry(int rank, String name, int guesses, Instant recordedAt) {}
