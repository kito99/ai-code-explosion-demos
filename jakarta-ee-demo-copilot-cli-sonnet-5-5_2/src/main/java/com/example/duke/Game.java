package com.example.duke;

import java.util.UUID;

public class Game {

    public static final int MIN_NUMBER = 1;
    public static final int MAX_NUMBER = 10;
    public static final int MAX_GUESSES = 5;

    public enum Status { ACTIVE, WON, LOST }

    public enum Hint { TOO_LOW, TOO_HIGH, CORRECT }

    private final String id = UUID.randomUUID().toString();
    private final int secret;
    private int guessesMade;
    private Status status = Status.ACTIVE;
    private boolean scorePosted;

    Game(int secret) {
        this.secret = secret;
    }

    public String id() {
        return id;
    }

    public synchronized Status status() {
        return status;
    }

    public synchronized int guessesMade() {
        return guessesMade;
    }

    public synchronized int guessesRemaining() {
        return MAX_GUESSES - guessesMade;
    }

    /** The secret is only revealed once the game has been lost. */
    public synchronized Integer revealedSecret() {
        return status == Status.LOST ? secret : null;
    }

    synchronized Hint guess(int value) {
        if (status != Status.ACTIVE) {
            throw new GameException(409, "This game is over. Start a new game to keep playing!");
        }
        guessesMade++;
        Hint hint = value == secret ? Hint.CORRECT : value < secret ? Hint.TOO_LOW : Hint.TOO_HIGH;
        if (hint == Hint.CORRECT) {
            status = Status.WON;
        } else if (guessesMade >= MAX_GUESSES) {
            status = Status.LOST;
        }
        return hint;
    }

    /** Claims the right to post this game's score; true only once, and only for a won game. */
    synchronized boolean claimScorePosting() {
        if (status != Status.WON) {
            throw new GameException(409, "Only a won game can be posted to the leaderboard.");
        }
        if (scorePosted) {
            throw new GameException(409, "This score has already been posted.");
        }
        scorePosted = true;
        return true;
    }

    synchronized void releaseScorePosting() {
        scorePosted = false;
    }
}
