package com.example.guessgame;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard")
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String playerName;

    @Column(nullable = false)
    private int guessCount;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    protected LeaderboardEntry() {}

    public LeaderboardEntry(String playerName, int guessCount) {
        this.playerName = playerName;
        this.guessCount = guessCount;
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPlayerName() { return playerName; }
    public int getGuessCount() { return guessCount; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
