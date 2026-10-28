CREATE TABLE IF NOT EXISTS leaderboard_entry (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(30) NOT NULL,
    guesses    INTEGER NOT NULL CHECK (guesses BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_leaderboard_rank ON leaderboard_entry (guesses, id);
