package tech.virtua.duke;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

/// Content types for the assets and responses the game serves.
public enum MediaType {

    HTML("text/html; charset=utf-8", ".html"),

    CSS("text/css; charset=utf-8", ".css"),

    JAVASCRIPT("text/javascript; charset=utf-8", ".js"),

    SVG("image/svg+xml", ".svg"),

    JSON("application/json; charset=utf-8", ".json");

    private final String headerValue;
    private final String fileExtension;

    MediaType(final String headerValue, final String fileExtension) {
        this.headerValue = headerValue;
        this.fileExtension = fileExtension;
    }

    /// The value for the `Content-Type` header.
    public String headerValue() {
        return headerValue;
    }

    /// The type matching the extension of `fileName`.
    ///
    /// @throws UnknownMediaTypeException when no type claims that extension
    public static MediaType forFileName(final String fileName) {
        requireNonNull(fileName);
        return Arrays.stream(values())
                .filter(candidate -> fileName.endsWith(candidate.fileExtension))
                .findFirst()
                .orElseThrow(() -> new UnknownMediaTypeException(fileName));
    }
}
