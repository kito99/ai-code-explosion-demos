package com.example.duke.leaderboard;

import com.example.duke.game.GameError;
import com.example.duke.game.GameException;

/// A validated leaderboard name: surrounding whitespace removed, 1 to 30 characters.
public record PlayerName(String value) {

    public static final int MAX_LENGTH = 30;

    public PlayerName {
        final var trimmed = value == null ? "" : value.strip();
        if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new GameException(GameError.INVALID_NAME,
                    "Please enter a name of 1 to %d characters.".formatted(MAX_LENGTH));
        }
        value = trimmed;
    }
}
