CREATE TABLE leaderboard_entry (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    player_name VARCHAR(30)              NOT NULL CHECK (btrim(player_name) <> ''),
    guess_count INTEGER                  NOT NULL CHECK (guess_count BETWEEN 1 AND 5),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Matches the leaderboard ordering exactly, so the top-10 read never sorts.
CREATE INDEX leaderboard_entry_ranking_idx
    ON leaderboard_entry (guess_count ASC, recorded_at ASC, id ASC);
