package com.example.duke.game;

import com.example.duke.leaderboard.LeaderboardService;
import com.example.duke.leaderboard.PostedScore;
import com.example.duke.leaderboard.PlayerName;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

import static com.example.duke.game.GameRules.isWithinRange;

/// Drives a round from start to win or loss, and hands a win to the leaderboard.
///
/// Logs here carry ids, counts and outcomes only; the player's name stays out of telemetry.
@Service
public class GameService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameService.class);

    private final GameStore store;
    private final SecretNumberGenerator secretNumberGenerator;
    private final LeaderboardService leaderboardService;
    private final GameMetrics metrics;
    private final Clock clock;

    public GameService(final GameStore store,
                       final SecretNumberGenerator secretNumberGenerator,
                       final LeaderboardService leaderboardService,
                       final GameMetrics metrics,
                       final Clock clock) {
        this.store = store;
        this.secretNumberGenerator = secretNumberGenerator;
        this.leaderboardService = leaderboardService;
        this.metrics = metrics;
        this.clock = clock;
    }

    /// Starts a round: Duke draws a number and the guess count resets to zero.
    @Observed(name = "duke.game.start", contextualName = "start-game")
    public Game startGame() {
        final var game = store.add(Game.start(UUID.randomUUID(), secretNumberGenerator.nextSecretNumber(), clock.instant()));
        metrics.gameStarted();
        LOGGER.info("Game {} started", game.id());
        return game;
    }

    /// Applies a guess to a round. An out-of-range guess is rejected without counting against the limit.
    @Observed(name = "duke.game.guess", contextualName = "submit-guess")
    public GuessResult submitGuess(final UUID gameId, final int guess) {
        if (!isWithinRange(guess)) {
            metrics.guessRejected();
            LOGGER.info("Game {} rejected an out-of-range guess", gameId);
            throw new InvalidGuessException(guess);
        }
        final var game = store.update(gameId, current -> current.withGuess(guess));
        final var outcome = GuessOutcome.of(guess, game.secretNumber());
        metrics.guessAccepted(outcome);
        if (game.status().isFinished()) {
            metrics.gameFinished(game.status(), game.guessesMade());
        }
        LOGGER.info("Game {} guess {} of {} was {}, game is {}",
                gameId, game.guessesMade(), GameRules.MAX_GUESSES, outcome, game.status());
        return new GuessResult(game, outcome);
    }

    /// Records a won round on the leaderboard under `rawName`, using the guess count the server holds.
    @Observed(name = "duke.game.post-score", contextualName = "post-score")
    public PostedScore postScore(final UUID gameId, final String rawName) {
        final var playerName = PlayerName.of(rawName);
        // The claim is taken first so that concurrent posts of one win cannot both get through,
        // and given back if the win does not reach the database, so a retry is still possible.
        final var game = store.update(gameId, Game::withScorePosted);
        try {
            final var postedScore = leaderboardService.record(playerName, game.guessesMade());
            metrics.scorePosted();
            LOGGER.info("Game {} posted a win of {} guesses to the leaderboard", gameId, game.guessesMade());
            return postedScore;
        } catch (final RuntimeException recordingFailure) {
            releaseScorePosting(gameId, recordingFailure);
            throw recordingFailure;
        }
    }

    private void releaseScorePosting(final UUID gameId, final RuntimeException recordingFailure) {
        try {
            store.update(gameId, Game::withScorePostingReleased);
            LOGGER.warn("Game {} could not be recorded, so the win can be posted again", gameId);
        } catch (final RuntimeException releaseFailure) {
            recordingFailure.addSuppressed(releaseFailure);
        }
    }
}
