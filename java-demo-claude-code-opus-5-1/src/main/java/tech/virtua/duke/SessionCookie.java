package tech.virtua.duke;

import com.sun.net.httpserver.Headers;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/// Reads and mints the cookie that ties a browser to its game.
public final class SessionCookie {

    private static final String COOKIE_NAME = "duke_session";
    private static final String COOKIE_HEADER = "Cookie";
    private static final String SET_COOKIE_HEADER = "Set-Cookie";
    private static final int ID_BYTES = 16;

    /// Ids are 16 random bytes in unpadded URL-safe base 64. Anything else is
    /// ignored rather than trusted as a map key.
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_-]{22}");

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SessionCookie() {
    }

    /// The name of the response header that carries the cookie.
    public static String setCookieHeaderName() {
        return SET_COOKIE_HEADER;
    }

    /// The session id in `requestHeaders`, if the browser sent a well-formed one.
    public static Optional<String> readFrom(final Headers requestHeaders) {
        requireNonNull(requestHeaders);
        final List<String> cookieHeaders = requireNonNullElse(requestHeaders.get(COOKIE_HEADER), List.of());
        return cookieHeaders.stream()
                .flatMap(header -> Arrays.stream(header.split(";")))
                .map(String::strip)
                .filter(pair -> pair.startsWith(COOKIE_NAME + "="))
                .map(pair -> pair.substring(COOKIE_NAME.length() + 1))
                .filter(id -> VALID_ID.matcher(id).matches())
                .findFirst();
    }

    /// A fresh, unguessable session id.
    public static String newSessionId() {
        final var bytes = new byte[ID_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    /// The `Set-Cookie` value that hands `sessionId` back to the browser. The
    /// cookie is a session cookie on purpose: nothing persists between visits.
    public static String headerValueFor(final String sessionId) {
        return format("%s=%s; Path=/; HttpOnly; SameSite=Lax", COOKIE_NAME, requireNonNull(sessionId));
    }
}
