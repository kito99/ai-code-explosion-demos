package com.example.duke.game;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class GameService {

    private final GameStore gameStore;
    private final SecretNumberGenerator secretNumberGenerator;

    public GameService(final GameStore gameStore, final SecretNumberGenerator secretNumberGenerator) {
        this.gameStore = gameStore;
        this.secretNumberGenerator = secretNumberGenerator;
    }

    public GameSnapshot startGame() {
        final var game = new Game(UUID.randomUUID().toString(), secretNumberGenerator.nextSecret());
        gameStore.add(game);
        return game.snapshot();
    }

    public GuessResult guess(final String gameId, final int value) {
        final var game = findGame(gameId);
        final var outcome = game.guess(value);
        return new GuessResult(outcome, game.snapshot());
    }

    public Game findGame(final String gameId) {
        return gameStore.find(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }
}
