package com.example.guess.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.Instant;

/**
 * Persistent leaderboard entry. One row per winning game.
 * FR-15: stores name, guesses taken, and when it was recorded.
 */
@Entity
@Table(name = "leaderboard_entry",
       indexes = @Index(name = "idx_leaderboard_guesses_recorded",
                        columnList = "guesses_taken ASC, recorded_at ASC"))
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "leaderboard_seq")
    @SequenceGenerator(name = "leaderboard_seq", sequenceName = "leaderboard_entry_seq", allocationSize = 50)
    private Long id;

    /** FR-14: name trimmed, 1–30 visible characters. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "player_name", nullable = false, length = 30)
    private String playerName;

    /** FR-15: number of accepted guesses the win took (1–5). */
    @Min(1) @Max(5)
    @Column(name = "guesses_taken", nullable = false)
    private int guessesTaken;

    /** FR-15: timestamp when the entry was recorded. */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected LeaderboardEntry() {}

    public LeaderboardEntry(String playerName, int guessesTaken, Instant recordedAt) {
        this.playerName   = playerName;
        this.guessesTaken = guessesTaken;
        this.recordedAt   = recordedAt;
    }

    public Long    getId()           { return id; }
    public String  getPlayerName()   { return playerName; }
    public int     getGuessesTaken() { return guessesTaken; }
    public Instant getRecordedAt()   { return recordedAt; }
}
