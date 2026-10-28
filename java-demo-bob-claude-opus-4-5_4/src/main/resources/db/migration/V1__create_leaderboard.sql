CREATE TABLE leaderboard (
    id          BIGSERIAL    PRIMARY KEY,
    player_name VARCHAR(30)  NOT NULL,
    guess_count INT          NOT NULL CHECK (guess_count >= 1 AND guess_count <= 5),
    recorded_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leaderboard_order ON leaderboard (guess_count ASC, recorded_at ASC);
