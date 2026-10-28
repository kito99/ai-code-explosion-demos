package com.example.duke.leaderboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

/// One posted win. Every winning game creates its own entry, and nothing ever overwrites or
/// removes one.
@Entity
@Table(name = "leaderboard_entry")
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_name", nullable = false, length = PlayerName.MAX_LENGTH)
    private String playerName;

    @Column(name = "guess_count", nullable = false)
    private int guessCount;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /// Required by Jakarta Persistence.
    protected LeaderboardEntry() {
    }

    public LeaderboardEntry(final PlayerName playerName, final int guessCount, final Instant recordedAt) {
        this.playerName = requireNonNull(playerName, "playerName").value();
        this.guessCount = guessCount;
        this.recordedAt = requireNonNull(recordedAt, "recordedAt");
    }

    public Long id() {
        return id;
    }

    public String playerName() {
        return playerName;
    }

    public int guessCount() {
        return guessCount;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
