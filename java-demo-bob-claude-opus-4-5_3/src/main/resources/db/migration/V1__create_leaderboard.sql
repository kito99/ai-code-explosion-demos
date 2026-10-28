CREATE TABLE leaderboard_entries (
    id          BIGSERIAL PRIMARY KEY,
    player_name VARCHAR(30)              NOT NULL,
    guess_count INTEGER                  NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_leaderboard_rank ON leaderboard_entries (guess_count ASC, recorded_at ASC);
