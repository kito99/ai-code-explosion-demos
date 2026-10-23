package tech.virtua.duke;

import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/// One HTTP response, with just enough JSON reading for the tests to assert on
/// individual fields.
record ApiResponse(int statusCode, String contentType, String allow, String body) {

    ApiResponse {
        requireNonNull(body);
    }

    String text(final String field) {
        return match(format("\"%s\":\"((?:[^\"\\\\]|\\\\.)*)\"", field), field);
    }

    int number(final String field) {
        return Integer.parseInt(match(format("\"%s\":(-?\\d+)", field), field));
    }

    boolean isNull(final String field) {
        return body.contains(format("\"%s\":null", field));
    }

    private String match(final String regex, final String field) {
        final var matcher = Pattern.compile(regex).matcher(body);
        if (!matcher.find()) {
            throw new AssertionError(format("No field '%s' in: %s", field, body));
        }
        return matcher.group(1);
    }
}
