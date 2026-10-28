package com.example.duke;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leaderboard_entry")
public class ScoreEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_name", nullable = false, length = 30)
    private String playerName;

    @Column(nullable = false)
    private int guesses;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public ScoreEntry() {
    }

    public ScoreEntry(String playerName, int guesses, Instant recordedAt) {
        this.playerName = playerName;
        this.guesses = guesses;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getGuesses() {
        return guesses;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
