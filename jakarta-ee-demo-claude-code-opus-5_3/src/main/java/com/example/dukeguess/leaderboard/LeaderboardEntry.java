package com.example.dukeguess.leaderboard;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;

/// One winning result on the leaderboard. Entries are never updated or removed.
@Entity
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = PlayerName.MAXIMUM_LENGTH)
    private String playerName;

    @Min(1)
    private int guessCount;

    private Instant recordedAt;

    protected LeaderboardEntry() {
    }

    public LeaderboardEntry(final String playerName, final int guessCount, final Instant recordedAt) {
        this.playerName = playerName;
        this.guessCount = guessCount;
        this.recordedAt = recordedAt;
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
