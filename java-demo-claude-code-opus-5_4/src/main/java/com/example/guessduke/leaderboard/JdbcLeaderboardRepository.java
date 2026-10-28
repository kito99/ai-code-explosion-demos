package com.example.guessduke.leaderboard;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/// Keeps leaderboard entries in PostgreSQL so they outlive the app process.
@Repository
public class JdbcLeaderboardRepository implements LeaderboardRepository {

    private static final String INSERT_ENTRY = """
            INSERT INTO leaderboard_entry (player_name, guess_count)
            VALUES (:playerName, :guessCount)
            RETURNING id, player_name, guess_count, recorded_at
            """;

    /// Wins recorded in the same clock tick share a timestamp, so the id decides which
    /// came first. The ranking index happens to carry the id too, which makes `id ASC`
    /// look redundant; it is not, because only the ORDER BY guarantees the tie-break if
    /// the planner ever stops using that index.
    private static final String SELECT_BEST = """
            SELECT id, player_name, guess_count, recorded_at
            FROM leaderboard_entry
            ORDER BY guess_count ASC, recorded_at ASC, id ASC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public JdbcLeaderboardRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient");
    }

    @Override
    public LeaderboardEntry record(final PlayerName playerName, final int guessCount) {
        Objects.requireNonNull(playerName, "playerName");
        return jdbcClient.sql(INSERT_ENTRY)
                .param("playerName", playerName.value())
                .param("guessCount", guessCount)
                .query(JdbcLeaderboardRepository::toEntry)
                .single();
    }

    @Override
    public List<LeaderboardEntry> findBest(final int limit) {
        return jdbcClient.sql(SELECT_BEST)
                .param("limit", limit)
                .query(JdbcLeaderboardRepository::toEntry)
                .list();
    }

    private static LeaderboardEntry toEntry(final ResultSet row, final int rowNumber) throws SQLException {
        return new LeaderboardEntry(
                row.getLong("id"),
                row.getString("player_name"),
                row.getInt("guess_count"),
                row.getTimestamp("recorded_at").toInstant());
    }
}
