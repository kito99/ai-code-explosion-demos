package tech.virtua.duke.game;

/// How the player's last counted guess compared to the secret number.
public enum GuessFeedback {

    /// No guess has counted yet in this game.
    NONE,

    /// The guess was below the secret number.
    TOO_LOW,

    /// The guess was above the secret number.
    TOO_HIGH,

    /// The guess was the secret number.
    CORRECT
}
