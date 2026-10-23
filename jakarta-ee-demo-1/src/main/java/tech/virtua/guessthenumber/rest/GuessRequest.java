package tech.virtua.guessthenumber.rest;

/**
 * A submitted guess, carried as text rather than a number. The player can type
 * anything, and deciding what counts as a valid guess is the server's job.
 */
public class GuessRequest {

    private String guess;

    public String getGuess() {
        return guess;
    }

    public void setGuess(String guess) {
        this.guess = guess;
    }
}
