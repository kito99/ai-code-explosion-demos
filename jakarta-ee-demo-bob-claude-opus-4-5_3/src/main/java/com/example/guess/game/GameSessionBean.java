package com.example.guess.game;

import jakarta.enterprise.context.SessionScoped;

import java.io.Serializable;

/**
 * CDI session-scoped bean that owns the {@link GameSession} for a single browser session.
 * A new session starts automatically when the bean is first accessed (FR-1).
 */
@SessionScoped
public class GameSessionBean implements Serializable {

    private transient GameSession currentSession;

    /** Returns the current session, creating a new one if none exists. */
    public GameSession current() {
        if (currentSession == null) {
            currentSession = new GameSession();
        }
        return currentSession;
    }

    /** Replaces the current session with a fresh game (FR-12). */
    public GameSession newGame() {
        currentSession = new GameSession();
        return currentSession;
    }
}
