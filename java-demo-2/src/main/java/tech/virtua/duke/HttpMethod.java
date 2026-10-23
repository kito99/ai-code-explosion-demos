package tech.virtua.duke;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

/// The request methods this game cares about.
public enum HttpMethod {

    GET,

    POST,

    /// Any other method, which every route rejects.
    UNSUPPORTED;

    /// The constant matching `method`, or [#UNSUPPORTED] when it is not one the
    /// game handles.
    public static HttpMethod from(final String method) {
        requireNonNull(method);
        return Arrays.stream(values())
                .filter(candidate -> candidate.name().equalsIgnoreCase(method))
                .findFirst()
                .orElse(UNSUPPORTED);
    }
}
