package com.example.dukeguess.game;

/// Where a round stands: still playable, or over because the player found the number or ran out of guesses.
public enum GameStatus {

    IN_PROGRESS,
    WON,
    LOST;

    public boolean isOver() {
        return this != IN_PROGRESS;
    }
}
