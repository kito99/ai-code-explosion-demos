package com.example.duke.leaderboard;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class LeaderboardRepository {

    private final JdbcClient jdbc;

    public LeaderboardRepository(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(final String name, final int guesses, final Instant recordedAt) {
        jdbc.sql("INSERT INTO leaderboard_entry (player_name, guesses, recorded_at) VALUES (?, ?, ?)")
                .params(name, guesses, recordedAt.atOffset(ZoneOffset.UTC))
                .update();
    }

    public List<LeaderboardStanding> findBest(final int limit) {
        return jdbc.sql("""
                        SELECT player_name, guesses
                        FROM leaderboard_entry
                        ORDER BY guesses, recorded_at, id
                        LIMIT ?""")
                .param(limit)
                .query((row, index) -> new LeaderboardStanding(index + 1, row.getString("player_name"), row.getInt("guesses")))
                .list();
    }

    public void deleteAll() {
        jdbc.sql("DELETE FROM leaderboard_entry").update();
    }
}
