package tech.virtua.duke;

import static java.lang.String.format;

/// Thrown when a front-end asset is listed for serving but is not on the
/// classpath, which means the build left it out.
public class MissingAssetException extends RuntimeException {

    public MissingAssetException(final String resourcePath) {
        super(format("Front-end asset '%s' is missing from the classpath.", resourcePath));
    }

    public MissingAssetException(final String resourcePath, final Throwable cause) {
        super(format("Front-end asset '%s' could not be read.", resourcePath), cause);
    }
}
