package com.example.dukeguess.rest;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/// Builds the JSON body the screens show when a request is turned down.
final class FriendlyResponses {

    private FriendlyResponses() {
    }

    static Response of(final Response.Status status, final String message) {
        return Response.status(status)
                .type(APPLICATION_JSON)
                .entity(new ErrorResponse(message))
                .build();
    }
}
