package tech.virtua.duke;

/// The response codes this game sends.
public enum HttpStatus {

    OK(200),

    NOT_FOUND(404),

    METHOD_NOT_ALLOWED(405),

    PAYLOAD_TOO_LARGE(413),

    INTERNAL_SERVER_ERROR(500),

    SERVICE_UNAVAILABLE(503);

    private final int code;

    HttpStatus(final int code) {
        this.code = code;
    }

    /// The numeric status code.
    public int code() {
        return code;
    }
}
