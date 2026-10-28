package com.example.duke;

import static com.example.duke.GameRules.MAX_NAME_LENGTH;

/** Validates leaderboard names. */
public final class NameValidator {

    private NameValidator() {
    }

    /** Returns the trimmed name or throws when it is blank or longer than the limit. */
    public static String validate(final String rawName) {
        final var name = rawName == null ? "" : rawName.strip();
        if (name.isEmpty()) {
            throw new InvalidNameException("Please enter a name to post your score.");
        }
        if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
            throw new InvalidNameException(String.format(
                    "Please keep your name to %d characters or fewer.", MAX_NAME_LENGTH));
        }
        return name;
    }
}
