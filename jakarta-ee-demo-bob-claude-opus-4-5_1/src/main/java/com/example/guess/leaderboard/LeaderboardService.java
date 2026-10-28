package com.example.guess.leaderboard;

import com.example.guess.entity.LeaderboardEntry;
import com.example.guess.model.LeaderboardEntryView;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records and retrieves leaderboard entries via JPA.
 */
@ApplicationScoped
public class LeaderboardService {

    private static final int TOP_N = 10;

    @PersistenceContext(unitName = "guessPU")
    private EntityManager entityManager;

    /**
     * Persists a new winning result.
     */
    @Transactional
    @WithSpan("leaderboard.record")
    public void record(String playerName, int guessCount) {
        var entry = new LeaderboardEntry(playerName, guessCount, Instant.now());
        entityManager.persist(entry);
    }

    /**
     * Returns the top 10 entries ordered by fewest guesses, ties broken by
     * oldest recorded first.
     */
    @Transactional
    @WithSpan("leaderboard.fetch")
    public List<LeaderboardEntryView> fetchTop() {
        var results = entityManager.createQuery(
                "SELECT e FROM LeaderboardEntry e ORDER BY e.guessCount ASC, e.recordedAt ASC",
                LeaderboardEntry.class)
                .setMaxResults(TOP_N)
                .getResultList();

        var rank = new AtomicInteger(1);
        return results.stream()
                .map(e -> new LeaderboardEntryView(
                        rank.getAndIncrement(),
                        e.getPlayerName(),
                        e.getGuessCount(),
                        e.getRecordedAt()))
                .toList();
    }
}
