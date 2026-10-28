package com.example.duke;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class Dtos {

    private Dtos() {
    }

    /** The guess arrives as text so that "abc" or "3.5" get the same friendly message as "11". */
    public record GuessRequest(
            @NotNull(message = "Please enter a whole number from 1 to 10.")
            @Pattern(regexp = "\\s*(10|[1-9])\\s*", message = "Please enter a whole number from 1 to 10.")
            String guess) {

        int value() {
            return Integer.parseInt(guess.strip());
        }
    }

    public record ScoreRequest(
            @NotBlank(message = "Please enter a name to post your score.")
            @Size(max = 30, message = "Please keep your name to 30 characters or fewer.")
            String name) {

        public ScoreRequest {
            name = name == null ? null : name.strip();
        }
    }

    public record GameView(String id, String status, int guessesMade, int guessesRemaining,
                           int maxGuesses, Integer secretNumber) {

        static GameView of(Game game) {
            return new GameView(game.id(), game.status().name(), game.guessesMade(),
                    game.guessesRemaining(), Game.MAX_GUESSES, game.revealedSecret());
        }
    }

    public record GuessResponse(GameView game, String hint, String message) {
    }

    public record ScoreRow(int rank, String name, int guesses) {
    }

    public record ErrorResponse(String message) {
    }
}
