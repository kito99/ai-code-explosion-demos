package com.example.dukeguess.it;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/// Talks to the running app the way a browser does, carrying one session cookie
/// so each instance behaves like a single player.
final class GameClient implements AutoCloseable {

    private static final String BASE_URI =
            "http://localhost:%s/".formatted(System.getProperty("http.port", "9082"));
    private static final String SESSION_COOKIE = "JSESSIONID";

    private final Client client = ClientBuilder.newClient();
    private final Jsonb jsonb = JsonbBuilder.create();

    private String sessionCookieValue;

    Response get(final String path) {
        return remember(request(path).get());
    }

    Response post(final String path, final Object body) {
        return remember(request(path).post(Entity.entity(serialize(body), APPLICATION_JSON)));
    }

    /// Reads a JSON response body into a map of field name to value.
    @SuppressWarnings("unchecked")
    static Map<String, Object> bodyOf(final Response response) {
        return response.readEntity(Map.class);
    }

    private Invocation.Builder request(final String path) {
        final var builder = client.target(BASE_URI + path).request(APPLICATION_JSON);
        return Optional.ofNullable(sessionCookieValue)
                .map(value -> builder.cookie(SESSION_COOKIE, value))
                .orElse(builder);
    }

    private Response remember(final Response response) {
        final var cookie = response.getCookies().get(SESSION_COOKIE);
        if (cookie != null) {
            sessionCookieValue = cookie.getValue();
        }
        return response;
    }

    private String serialize(final Object body) {
        return body == null ? "{}" : jsonb.toJson(body);
    }

    @Override
    public void close() {
        client.close();
        try {
            jsonb.close();
        } catch (final Exception exception) {
            throw new IllegalStateException("Could not close the JSON binding context.", exception);
        }
    }
}
