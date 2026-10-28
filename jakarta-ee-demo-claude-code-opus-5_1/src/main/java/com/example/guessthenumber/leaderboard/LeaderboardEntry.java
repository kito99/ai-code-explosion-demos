package com.example.guessthenumber.leaderboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.time.Instant;

/// A win that a player posted. Entries are never updated or removed, so each win keeps
/// its own row.
@Entity
@Table(name = "leaderboard_entry")
@NamedQuery(
        name = LeaderboardEntry.FIND_BEST,
        query = """
                SELECT win FROM LeaderboardEntry win
                ORDER BY win.guessCount ASC, win.recordedAt ASC, win.id ASC
                """)
public class LeaderboardEntry {

    /// Orders wins by fewest guesses, breaking ties by which was recorded first. The
    /// identifier is the final tiebreaker because two entries can share a timestamp.
    public static final String FIND_BEST = "LeaderboardEntry.findBest";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "player_name", nullable = false, length = PlayerName.MAX_LENGTH)
    private String playerName;

    @Column(name = "guess_count", nullable = false)
    private int guessCount;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected LeaderboardEntry() {
        // Required by Jakarta Persistence.
    }

    LeaderboardEntry(final PlayerName playerName, final int guessCount, final Instant recordedAt) {
        this.playerName = playerName.value();
        this.guessCount = guessCount;
        this.recordedAt = recordedAt;
    }

    public String playerName() {
        return playerName;
    }

    public int guessCount() {
        return guessCount;
    }
}
