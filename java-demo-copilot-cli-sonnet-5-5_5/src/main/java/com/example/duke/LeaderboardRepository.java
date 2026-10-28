package com.example.duke;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class LeaderboardRepository {
    public static final int TOP = 10;

    private final JdbcClient jdbc;

    public LeaderboardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void add(String name, int guesses) {
        jdbc.sql("INSERT INTO leaderboard_entry (name, guesses) VALUES (:name, :guesses)")
                .param("name", name)
                .param("guesses", guesses)
                .update();
    }

    public List<LeaderboardEntry> top() {
        var rows = jdbc.sql("""
                SELECT name, guesses, recorded_at FROM leaderboard_entry
                ORDER BY guesses ASC, recorded_at ASC, id ASC
                LIMIT :limit
                """)
                .param("limit", TOP)
                .query((rs, n) -> new Object[] {
                        rs.getString("name"),
                        rs.getInt("guesses"),
                        rs.getTimestamp("recorded_at").toInstant() })
                .list();
        var result = new ArrayList<LeaderboardEntry>();
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            result.add(new LeaderboardEntry(i + 1, (String) r[0], (Integer) r[1], (java.time.Instant) r[2]));
        }
        return result;
    }
}
