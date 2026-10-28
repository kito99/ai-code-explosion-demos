package com.example.duke;

public final class NameValidator {
    public static final int MAX_LENGTH = 30;

    private NameValidator() {}

    /** Returns the trimmed name, or null if it is blank or longer than 30 characters. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.strip();
        if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            return null;
        }
        return trimmed;
    }
}
