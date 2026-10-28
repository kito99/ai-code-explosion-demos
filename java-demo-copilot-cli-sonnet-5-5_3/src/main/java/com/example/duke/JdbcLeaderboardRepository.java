package com.example.duke;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

/** PostgreSQL-backed leaderboard. */
public class JdbcLeaderboardRepository implements LeaderboardRepository {

    private static final String INSERT_SQL = """
            INSERT INTO leaderboard_entry (player_name, guesses)
            VALUES (?, ?)
            RETURNING id, player_name, guesses, recorded_at
            """;
    private static final String TOP_SQL = """
            SELECT id, player_name, guesses, recorded_at
            FROM leaderboard_entry
            ORDER BY guesses ASC, recorded_at ASC, id ASC
            LIMIT ?
            """;
    
    private final DataSource dataSource;

    public JdbcLeaderboardRepository(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public LeaderboardEntry save(final String playerName, final int guesses) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, playerName);
            statement.setInt(2, guesses);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return toEntry(resultSet);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not save the leaderboard entry.", exception);
        }
    }

    @Override
    public List<LeaderboardEntry> findTop(final int limit) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(TOP_SQL)) {
            statement.setInt(1, limit);
            try (var resultSet = statement.executeQuery()) {
                final var entries = new ArrayList<LeaderboardEntry>();
                while (resultSet.next()) {
                    entries.add(toEntry(resultSet));
                }
                return List.copyOf(entries);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Could not read the leaderboard.", exception);
        }
    }

    private static LeaderboardEntry toEntry(final ResultSet resultSet) throws SQLException {
        return new LeaderboardEntry(
                resultSet.getLong("id"),
                resultSet.getString("player_name"),
                resultSet.getInt("guesses"),
                resultSet.getObject("recorded_at", OffsetDateTime.class).toInstant());
    }
}
