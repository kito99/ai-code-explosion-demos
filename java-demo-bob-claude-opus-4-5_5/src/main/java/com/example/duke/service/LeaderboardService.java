package com.example.duke.service;

import com.example.duke.model.LeaderboardEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe, in-memory leaderboard.
 * Entries are shared across all users on this server instance (FR-20).
 */
@Service
public class LeaderboardService {

    private static final int MAX_ENTRIES = 10;
    private static final int MAX_NAME_LENGTH = 30;

    private final CopyOnWriteArrayList<LeaderboardEntry> entries = new CopyOnWriteArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    /**
     * Validates and records a winning result.
     *
     * @param name       the player's name (trimmed; 1–30 visible chars)
     * @param guessCount the number of guesses used to win
     * @return the recorded entry
     * @throws IllegalArgumentException if the name is invalid
     */
    public LeaderboardEntry record(String name, int guessCount) {
        var trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Please enter a name before posting your score.");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Name must be 30 characters or fewer. Yours is %d characters.".formatted(trimmed.length()));
        }

        var entry = new LeaderboardEntry(idSequence.getAndIncrement(), trimmed, guessCount, Instant.now());
        entries.add(entry);
        return entry;
    }

    /**
     * Returns the top-10 entries: fewest guesses first, ties broken by insertion order (id).
     */
    public List<LeaderboardEntry> getTopEntries() {
        return entries.stream()
                .sorted(Comparator.comparingInt(LeaderboardEntry::guessCount)
                        .thenComparingLong(LeaderboardEntry::id))
                .limit(MAX_ENTRIES)
                .toList();
    }

    /** Removes all entries — used for testing. */
    public void clear() {
        entries.clear();
    }
}
