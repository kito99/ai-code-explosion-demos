package com.example.guessgame.leaderboard;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Persistent leaderboard entry recording a single win.
 */
@Entity
@Table(name = "leaderboard_entry")
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_name", nullable = false, length = 30)
    private String playerName;

    @Column(name = "guess_count", nullable = false)
    private int guessCount;

    @Column(name = "recorded_at", nullable = false)
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
