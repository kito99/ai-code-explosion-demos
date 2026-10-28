package com.example.duke;

import static com.example.duke.GameRules.LEADERBOARD_SIZE;
import static com.example.duke.GameRules.MAX_NUMBER;
import static com.example.duke.GameRules.MIN_NUMBER;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;

/** Application logic: runs games in memory and records wins on the leaderboard. */
public class GameService {

    private static final int MAX_TRACKED_GAMES = 10_000;
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,9}");

    private final LeaderboardRepository repository;
    private final IntSupplier secretNumberSource;
    private final ConcurrentHashMap<String, Game> games = new ConcurrentHashMap<>();
    private final Queue<String> gameOrder = new ConcurrentLinkedQueue<>();

    public GameService(final LeaderboardRepository repository, final IntSupplier secretNumberSource) {
        this.repository = Objects.requireNonNull(repository);
        this.secretNumberSource = Objects.requireNonNull(secretNumberSource);
    }

    /** Uniformly random secret number between the game's bounds, inclusive. */
    public static int randomSecretNumber() {
        return ThreadLocalRandom.current().nextInt(MIN_NUMBER, MAX_NUMBER + 1);
    }

    public Game startGame() {
        final var game = new Game(UUID.randomUUID().toString(), secretNumberSource.getAsInt());
        games.put(game.id(), game);
        gameOrder.add(game.id());
        evictOldestGames();
        return game;
    }

    public GuessResult guess(final String gameId, final String rawGuess) {
        final var game = find(gameId);
        return game.guess(parseGuess(rawGuess));
    }

    public Game find(final String gameId) {
        return Optional.ofNullable(gameId)
                .map(games::get)
                .orElseThrow(() -> new GameNotFoundException(String.valueOf(gameId)));
    }

    public LeaderboardEntry postScore(final String gameId, final String rawName) {
        final var game = find(gameId);
        final var name = NameValidator.validate(rawName);
        final var guesses = game.claimScore();
        try {
            return repository.save(name, guesses);
        } catch (RuntimeException exception) {
            game.releaseScore();
            throw exception;
        }
    }

    public List<LeaderboardEntry> leaderboard() {
        return repository.findTop(LEADERBOARD_SIZE);
    }

    private static int parseGuess(final String rawGuess) {
        final var trimmed = rawGuess == null ? "" : rawGuess.strip();
        if (!WHOLE_NUMBER.matcher(trimmed).matches()) {
            throw new InvalidGuessException(String.format(
                    "Please enter a whole number from %d to %d.", MIN_NUMBER, MAX_NUMBER));
        }
        return Integer.parseInt(trimmed);
    }

    private void evictOldestGames() {
        while (games.size() > MAX_TRACKED_GAMES) {
            final var oldest = gameOrder.poll();
            if (oldest == null) {
                return;
            }
            games.remove(oldest);
        }
    }
}
