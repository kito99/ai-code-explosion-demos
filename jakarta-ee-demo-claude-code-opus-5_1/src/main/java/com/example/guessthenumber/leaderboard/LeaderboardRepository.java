package com.example.guessthenumber.leaderboard;

import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

/// Reads and writes leaderboard entries. Entries outlive the application process because
/// they live in the database.
@Singleton
public class LeaderboardRepository {

    @PersistenceContext(unitName = "leaderboard")
    private EntityManager entityManager;

    @Transactional
    public LeaderboardEntry add(final PlayerName playerName, final int guessCount, final Instant recordedAt) {
        final var entry = new LeaderboardEntry(playerName, guessCount, recordedAt);
        entityManager.persist(entry);
        return entry;
    }

    /// The best entries, fewest guesses first, at most `limit` of them.
    public List<LeaderboardEntry> findBest(final int limit) {
        return entityManager.createNamedQuery(LeaderboardEntry.FIND_BEST, LeaderboardEntry.class)
                .setMaxResults(limit)
                .getResultList();
    }
}
