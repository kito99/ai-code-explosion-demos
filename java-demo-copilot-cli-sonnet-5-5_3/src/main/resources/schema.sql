CREATE TABLE IF NOT EXISTS leaderboard_entry (
    id BIGSERIAL PRIMARY KEY,
    player_name VARCHAR(30) NOT NULL,
    guesses INTEGER NOT NULL CHECK (guesses BETWEEN 1 AND 5),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX IF NOT EXISTS idx_leaderboard_entry_ranking
    ON leaderboard_entry (guesses, recorded_at, id);
