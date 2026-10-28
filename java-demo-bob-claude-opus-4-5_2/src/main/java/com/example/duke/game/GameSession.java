package com.example.duke.game;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable in-memory game session backing a single play-through.
 * Atomic fields let multiple threads read status and feedback safely;
 * the check-and-act on guess submission is synchronized in GameService.
 */
class GameSession {

    private final String gameId;
    final int secretNumber;

    private final AtomicInteger guessesUsed = new AtomicInteger(0);
    private final AtomicReference<GameStatus> status = new AtomicReference<>(GameStatus.ACTIVE);
    private final AtomicReference<String> lastFeedback = new AtomicReference<>("");

    GameSession(String gameId, int secretNumber) {
        this.gameId = gameId;
        this.secretNumber = secretNumber;
    }

    int secretNumber() { return secretNumber; }

    void incrementGuesses() { guessesUsed.incrementAndGet(); }

    int guessesUsed() { return guessesUsed.get(); }

    GameStatus status() { return status.get(); }

    void setStatus(GameStatus newStatus) { status.set(newStatus); }

    void setLastFeedback(String feedback) { lastFeedback.set(feedback); }

    Game toGame() {
        var used = guessesUsed.get();
        var currentStatus = status.get();
        // Reveal the secret only when the game is lost; never expose it while active or won.
        Integer revealed = currentStatus == GameStatus.LOST ? secretNumber : null;
        return new Game(
                gameId,
                used,
                Math.max(0, Game.MAX_GUESSES - used),
                currentStatus,
                lastFeedback.get(),
                revealed
        );
    }

    GameResult toResult() {
        return new GameResult(gameId, status.get(), guessesUsed.get());
    }
}
