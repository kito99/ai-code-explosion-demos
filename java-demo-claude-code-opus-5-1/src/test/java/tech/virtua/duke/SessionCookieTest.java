package tech.virtua.duke;

import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionCookieTest {

    private static Headers withCookieHeader(final String value) {
        final var headers = new Headers();
        headers.add("Cookie", value);
        return headers;
    }

    @Test
    void readsTheSessionIdOutOfACrowdedCookieHeader() {
        final var sessionId = SessionCookie.newSessionId();
        final var headers = withCookieHeader("theme=dark; duke_session=%s; consent=yes".formatted(sessionId));

        assertEquals(sessionId, SessionCookie.readFrom(headers).orElseThrow());
    }

    @Test
    void findsNothingWhenNoCookieWasSent() {
        assertTrue(SessionCookie.readFrom(new Headers()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "duke_session=",
            "duke_session=../../etc/passwd",
            "duke_session=short",
            "duke_session=way-too-long-to-be-one-of-ours",
            "other_session=AAAAAAAAAAAAAAAAAAAAAA"})
    void ignoresIdsItDidNotMint(final String cookieHeader) {
        assertTrue(SessionCookie.readFrom(withCookieHeader(cookieHeader)).isEmpty());
    }

    @Test
    void mintsADifferentIdEachTime() {
        assertNotEquals(SessionCookie.newSessionId(), SessionCookie.newSessionId());
    }

    @Test
    void mintsIdsItAcceptsBack() {
        final var sessionId = SessionCookie.newSessionId();

        assertEquals(sessionId, SessionCookie.readFrom(withCookieHeader("duke_session=" + sessionId)).orElseThrow());
    }

    @Test
    void sendsTheCookieBackScopedToTheSiteAndHiddenFromScripts() {
        final var headerValue = SessionCookie.headerValueFor("AAAAAAAAAAAAAAAAAAAAAA");

        assertEquals("duke_session=AAAAAAAAAAAAAAAAAAAAAA; Path=/; HttpOnly; SameSite=Lax", headerValue);
    }
}
