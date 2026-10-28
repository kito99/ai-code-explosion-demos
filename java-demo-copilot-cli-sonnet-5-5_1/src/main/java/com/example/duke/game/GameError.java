package com.example.duke.game;

/// Reasons a game action is refused.
public enum GameError {
    INVALID_GUESS,
    INVALID_NAME,
    GAME_NOT_FOUND,
    GAME_FINISHED,
    GAME_NOT_WON,
    SCORE_ALREADY_POSTED
}
