package com.example.guess.game;

/**
 * Mutable state for a single game session. Not thread-safe on its own;
 * callers coordinate via {@link GameSessionStore}.
 */
public final class GameSession {

    private final String id;
    private final int secretNumber;
    private int guessCount;
    private boolean won;
    private boolean lost;

    GameSession(String id, int secretNumber) {
        this.id = id;
        this.secretNumber = secretNumber;
    }

    public String id() { return id; }

    public int secretNumber() { return secretNumber; }

    public int guessCount() { return guessCount; }

    public int guessesRemaining() {
        return Math.max(0, GameSessionStore.MAX_GUESSES - guessCount);
    }

    public boolean isActive() { return !won && !lost; }

    public boolean isWon() { return won; }

    public boolean isLost() { return lost; }

    void incrementGuesses() { guessCount++; }

    void markWon() { won = true; }

    void markLost() { lost = true; }
}
