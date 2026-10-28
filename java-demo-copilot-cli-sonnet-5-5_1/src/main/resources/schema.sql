CREATE TABLE IF NOT EXISTS leaderboard_entry (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    player_name VARCHAR(30) NOT NULL,
    guesses     INTEGER     NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS leaderboard_entry_ranking
    ON leaderboard_entry (guesses, recorded_at, id);
