package com.example.duke.leaderboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;

/// A single winning result, kept in the database so the leaderboard outlives the app process.
@Entity
@Table(name = "leaderboard_entry",
        indexes = @Index(name = "idx_leaderboard_best_first", columnList = "guesses, recorded_at, id"))
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "player_name", nullable = false, length = PlayerName.MAX_LENGTH)
    private String playerName;

    @Column(name = "guesses", nullable = false)
    private int guesses;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected LeaderboardEntry() {
        // Required by JPA.
    }

    public LeaderboardEntry(final PlayerName playerName, final int guesses, final Instant recordedAt) {
        this.playerName = playerName.value();
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
