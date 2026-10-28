package com.example.dukeguess.leaderboard;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/// Reads and writes leaderboard rows.
///
/// Entries are append-only: a posted win is never updated or deleted, so the table needs
/// nothing beyond an insert and an ordered read.
@Repository
public class LeaderboardRepository {

    private static final String INSERT_ENTRY = """
            INSERT INTO leaderboard_entry (player_name, guesses, recorded_at)
            VALUES (:playerName, :guesses, :recordedAt)
            """;

    /// Ordered by fewest guesses first, then by which row was recorded first. The id
    /// settles rows sharing a timestamp, so the order is total and stable.
    private static final String SELECT_BEST = """
            SELECT player_name, guesses, recorded_at
            FROM leaderboard_entry
            ORDER BY guesses ASC, recorded_at ASC, id ASC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public LeaderboardRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insert(final LeaderboardEntry entry) {
        jdbcClient.sql(INSERT_ENTRY)
                .param("playerName", entry.name().value())
                .param("guesses", entry.guesses())
                .param("recordedAt", Timestamp.from(entry.recordedAt()))
                .update();
    }

    public List<LeaderboardEntry> findBest(final int limit) {
        return jdbcClient.sql(SELECT_BEST)
                .param("limit", limit)
                .query((rs, _) -> new LeaderboardEntry(
                        new PlayerName(rs.getString("player_name")),
                        rs.getInt("guesses"),
                        rs.getTimestamp("recorded_at").toInstant()))
                .list();
    }
}
