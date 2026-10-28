-- Posted wins. Rows are append-only: the app never updates or deletes an entry.
CREATE TABLE leaderboard_entry (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    player_name VARCHAR(30) NOT NULL,
    guesses     SMALLINT    NOT NULL CHECK (guesses BETWEEN 1 AND 5),
    recorded_at TIMESTAMPTZ NOT NULL
);

-- Matches the leaderboard read: fewest guesses first, ties to whoever posted first.
CREATE INDEX leaderboard_entry_ranking_idx
    ON leaderboard_entry (guesses ASC, recorded_at ASC, id ASC);
