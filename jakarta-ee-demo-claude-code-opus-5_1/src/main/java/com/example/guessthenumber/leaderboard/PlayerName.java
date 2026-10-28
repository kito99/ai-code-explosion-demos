package com.example.guessthenumber.leaderboard;

/// The name a winner puts on the leaderboard, trimmed of surrounding whitespace. It is
/// not an account: two players may use the same name.
public record PlayerName(String value) {

    public static final int MAX_LENGTH = 30;

    public PlayerName {
        value = trimmed(value);
    }

    private static String trimmed(final String raw) {
        if (raw == null) {
            throw new InvalidPlayerNameException();
        }
        final var trimmed = raw.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            throw new InvalidPlayerNameException();
        }
        return trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
