package com.example.duke;

import java.util.List;

import jakarta.data.Limit;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Repository;

@Repository(dataStore = "DukeStore")
public interface ScoreRepository {

    @Insert
    ScoreEntry add(ScoreEntry entry);

    // Fewest guesses first; ties go to whoever was recorded first (id breaks identical timestamps).
    @Find
    @OrderBy("guesses")
    @OrderBy("recordedAt")
    @OrderBy("id")
    List<ScoreEntry> best(Limit limit);
}
