package com.example.duke.leaderboard;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "score")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = LeaderboardService.MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false)
    private int guesses;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected Score() {
    }

    public Score(final String name, final int guesses, final Instant recordedAt) {
        this.name = name;
        this.guesses = guesses;
        this.recordedAt = recordedAt;
    }

    public String getName() {
        return name;
    }

    public int getGuesses() {
        return guesses;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
