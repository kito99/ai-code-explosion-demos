package com.example.guessgame.game;

import org.springframework.stereotype.Service;

/**
 * Coordinates game-lifecycle operations for a single player session.
 */
@Service
public class GameService {

    private final GameSessionState sessionState;

    public GameService(GameSessionState sessionState) {
        this.sessionState = sessionState;
    }

    /** Starts a fresh game, discarding any previous state. */
    public GameSession newGame() {
        sessionState.reset();
        return sessionState.toSnapshot();
    }

    /** Returns the current state without mutating anything. */
    public GameSession currentGame() {
        return sessionState.toSnapshot();
    }

    /**
     * Submits a guess and returns the updated session snapshot.
     *
     * @param guess the number the player guessed
     */
    public GameSession guess(int guess) {
        sessionState.evaluate(guess);
        return sessionState.toSnapshot();
    }

}
