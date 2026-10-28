package com.example.duke;

import com.example.duke.Dtos.GameView;
import com.example.duke.Dtos.GuessRequest;
import com.example.duke.Dtos.GuessResponse;
import com.example.duke.Dtos.ScoreRequest;
import com.example.duke.Dtos.ScoreRow;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameResource {

    @Inject
    GameService service;

    @POST
    @Path("games")
    @Operation(summary = "Start a new game; Duke picks a secret number from 1 to 10")
    public Response start() {
        return Response.status(Response.Status.CREATED).entity(GameView.of(service.start())).build();
    }

    @POST
    @Path("games/{id}/guesses")
    @Operation(summary = "Make a guess in a game")
    public GuessResponse guess(@PathParam("id") String id, @NotNull(message = "Please enter a whole number from 1 to 10.") @Valid GuessRequest request) {
        Game.Hint hint = service.guess(id, request.value());
        Game game = service.find(id);
        return new GuessResponse(GameView.of(game), hint.name(), message(hint, game, request.value()));
    }

    @POST
    @Path("games/{id}/score")
    @Operation(summary = "Post the score of a won game to the leaderboard")
    public Response postScore(@PathParam("id") String id, @NotNull(message = "Please enter a name to post your score.") @Valid ScoreRequest request) {
        ScoreEntry entry = service.postScore(id, request.name());
        return Response.status(Response.Status.CREATED)
                .entity(new ScoreRow(0, entry.getPlayerName(), entry.getGuesses())).build();
    }

    @GET
    @Path("leaderboard")
    @Operation(summary = "The top 10 winning results, fewest guesses first")
    public List<ScoreRow> leaderboard() {
        List<ScoreEntry> best = service.leaderboard();
        return IntStream.range(0, best.size())
                .mapToObj(i -> new ScoreRow(i + 1, best.get(i).getPlayerName(), best.get(i).getGuesses()))
                .toList();
    }

    private static String message(Game.Hint hint, Game game, int value) {
        int made = game.guessesMade();
        String guesses = made == 1 ? "1 guess" : made + " guesses";
        return switch (hint) {
            case CORRECT -> "You got it! Duke was thinking of " + value + ". It took you " + guesses
                    + ". Enter your name to post your score, or start a new game.";
            case TOO_LOW, TOO_HIGH -> {
                String direction = hint == Game.Hint.TOO_LOW ? "too low" : "too high";
                yield game.guessesRemaining() > 0
                        ? value + " is " + direction + ". Try again!"
                        : value + " is " + direction + ". Out of guesses! Duke was thinking of "
                                + game.revealedSecret() + ". Start a new game to try again.";
            }
        };
    }
}
