package com.example.guessduke.web;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

import com.example.guessduke.game.GuessingGame;

/// Keeps one player's round in their HTTP session, so a round belongs to the
/// player who started it and nothing about it survives the session.
@Component
public class GameSessionStore {

    private static final String GAME_ATTRIBUTE = "guessduke.currentGame";

    /// Replaces whatever round the session held with the given one.
    public GuessingGame hold(final HttpSession session, final GuessingGame game) {
        session.setAttribute(GAME_ATTRIBUTE, game);
        return game;
    }

    /// Returns the round in progress, or empty when the session has none.
    public Optional<GuessingGame> find(final HttpSession session) {
        return Optional.ofNullable(session.getAttribute(GAME_ATTRIBUTE)).map(GuessingGame.class::cast);
    }
}
