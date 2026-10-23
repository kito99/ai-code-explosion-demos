package tech.virtua.duke;

import static java.lang.String.format;

/// Thrown when an asset's extension does not map to a known [MediaType], which
/// means the asset list and the type list have drifted apart.
public class UnknownMediaTypeException extends RuntimeException {

    public UnknownMediaTypeException(final String fileName) {
        super(format("No media type is registered for '%s'.", fileName));
    }
}
