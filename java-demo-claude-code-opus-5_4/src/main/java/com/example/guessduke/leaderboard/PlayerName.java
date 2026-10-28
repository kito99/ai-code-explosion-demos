package com.example.guessduke.leaderboard;

import java.util.Objects;
import java.util.Optional;

import static com.example.guessduke.leaderboard.LeaderboardRules.MAX_NAME_LENGTH;

/// A name a player attached to a win, already trimmed and known to be postable.
/// It identifies nothing; two players may post under the same name.
public record PlayerName(String value) {

    public PlayerName {
        Objects.requireNonNull(value, "value");
    }

    /// Reads a postable name from raw player input, returning empty when nothing
    /// visible is left after trimming or when the trimmed name is too long.
    public static Optional<PlayerName> parse(final String rawName) {
        if (rawName == null) {
            return Optional.empty();
        }
        final var trimmed = rawName.trim();
        return trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH
                ? Optional.empty()
                : Optional.of(new PlayerName(trimmed));
    }
}
