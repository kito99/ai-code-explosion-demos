package com.example.duke.leaderboard;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persisted record of a single winning game result.
 * Each win creates its own entry; entries are never overwritten.
 */
@Entity
@Table(name = "leaderboard_entry")
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String playerName;

    @Column(nullable = false)
    private int guessCount;

    @Column(nullable = false)
    private Instant recordedAt;

    protected LeaderboardEntry() {}

    public LeaderboardEntry(String playerName, int guessCount, Instant recordedAt) {
        this.playerName = playerName;
        this.guessCount = guessCount;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public String getPlayerName() { return playerName; }
    public int getGuessCount() { return guessCount; }
    public Instant getRecordedAt() { return recordedAt; }
}
