package com.example.guessthenumber.game;

import jakarta.enterprise.context.SessionScoped;

import java.io.Serializable;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/// Holds the game the player in this HTTP session is playing. A game never outlives the
/// session, which is why reloading the page starts a fresh one.
@SessionScoped
public class CurrentGame implements Serializable {

    private Game game;

    public void replaceWith(final Game game) {
        this.game = requireNonNull(game, "game");
    }

    public Optional<Game> game() {
        return Optional.ofNullable(game);
    }

    /// @throws NoGameInProgressException if the player has not started a game yet
    public Game require() {
        return game().orElseThrow(NoGameInProgressException::new);
    }
}
