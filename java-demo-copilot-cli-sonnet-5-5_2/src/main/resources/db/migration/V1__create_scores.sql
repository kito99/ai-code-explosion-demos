CREATE TABLE score (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(30)              NOT NULL,
    guesses     INTEGER                  NOT NULL CHECK (guesses BETWEEN 1 AND 5),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_score_ranking ON score (guesses, recorded_at, id);
