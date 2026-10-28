package com.example.dukeguess.rest;

import com.example.dukeguess.game.GameSession;
import com.example.dukeguess.game.GuessOutOfRangeException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.example.dukeguess.game.GuessingGame.HIGHEST_NUMBER;
import static com.example.dukeguess.game.GuessingGame.LOWEST_NUMBER;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;

/// Plays the game: reads the current state, starts new games, and takes guesses.
@Path("/game")
@RequestScoped
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class GameResource {

    @Inject
    private GameSession gameSession;

    @GET
    public GameStateResponse currentGame() {
        return GameStateResponse.of(gameSession.currentGame());
    }

    @POST
    @Path("/new")
    public GameStateResponse newGame() {
        return GameStateResponse.of(gameSession.startNewGame());
    }

    @POST
    @Path("/guesses")
    public GameStateResponse guess(final GuessRequest request) {
        final var outcome = gameSession.guess(parseGuess(request));
        return GameStateResponse.of(gameSession.currentGame(), outcome);
    }

    /// Reads the typed guess, rejecting anything that is not a whole number in range
    /// before it can count against the guess limit.
    private static int parseGuess(final GuessRequest request) {
        final var typedGuess = request == null || request.guess() == null ? "" : request.guess().strip();
        try {
            return Integer.parseInt(typedGuess);
        } catch (final NumberFormatException _) {
            throw new GuessOutOfRangeException(
                    format("Please enter a whole number from %d to %d.", LOWEST_NUMBER, HIGHEST_NUMBER));
        }
    }
}
