package com.example.duke.leaderboard;

import jakarta.data.Limit;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

import java.util.List;

/// Jakarta Data repository over the posted wins. The data store is the `jdbc/leaderboard`
/// data source from `server.xml`, and Liberty's built-in provider creates the table from the
/// entity on first use.
///
/// Only an insert and a read are declared, because the game never edits or deletes an entry.
@Repository(dataStore = "jdbc/leaderboard")
public interface LeaderboardEntries extends DataRepository<LeaderboardEntry, Long> {

    @Insert
    LeaderboardEntry add(LeaderboardEntry entry);

    /// The best results, fewest guesses first. Ties go to whichever was recorded first, and
    /// the id breaks the rare tie between two entries recorded in the same instant.
    @Find
    @OrderBy(_LeaderboardEntry.GUESS_COUNT)
    @OrderBy(_LeaderboardEntry.RECORDED_AT)
    @OrderBy(_LeaderboardEntry.ID)
    List<LeaderboardEntry> bestResults(Limit limit);
}
