CREATE TABLE leaderboard_entry (
    id         BIGSERIAL PRIMARY KEY,
    player_name VARCHAR(30)  NOT NULL,
    guess_count INTEGER      NOT NULL,
    recorded_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
