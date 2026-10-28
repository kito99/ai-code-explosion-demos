package com.example.guessgame;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persisted record of a winning game result on the leaderboard.
 */
@Entity
@Table(name = "leaderboard_entries")
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

    protected LeaderboardEntry() {
    }

    public LeaderboardEntry(String playerName, int guessCount, Instant recordedAt) {
        this.playerName = playerName;
        this.guessCount = guessCount;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getGuessCount() {
        return guessCount;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
