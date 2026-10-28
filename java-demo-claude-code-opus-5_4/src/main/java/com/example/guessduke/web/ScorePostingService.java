package com.example.guessduke.web;

import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.example.guessduke.game.GuessingGame;
import com.example.guessduke.leaderboard.LeaderboardRepository;
import com.example.guessduke.leaderboard.PlayerName;

import static com.example.guessduke.game.GameStatus.WON;
import static com.example.guessduke.web.GameMessages.NAME_NOT_ACCEPTED;
import static com.example.guessduke.web.GameMessages.NO_SCORE_TO_POST;
import static com.example.guessduke.web.GameMessages.SCORE_ALREADY_POSTED;

/// Posts a won round to the leaderboard once the player supplies a usable name.
/// Rounds that were lost, names that are not postable, and repeat submissions all
/// record nothing.
@Service
public class ScorePostingService {

    private final LeaderboardRepository leaderboardRepository;

    public ScorePostingService(final LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = Objects.requireNonNull(leaderboardRepository, "leaderboardRepository");
    }

    /// Records the win under the name the player typed, or explains why it was not recorded.
    public ScoreSubmissionView postScore(final GuessingGame game, final String rawName) {
        if (game.status() != WON) {
            return ScoreSubmissionView.declined(NO_SCORE_TO_POST);
        }
        return PlayerName.parse(rawName)
                .map(playerName -> record(game, playerName))
                .orElseGet(() -> ScoreSubmissionView.declined(NAME_NOT_ACCEPTED));
    }

    private ScoreSubmissionView record(final GuessingGame game, final PlayerName playerName) {
        if (!game.claimScorePosting()) {
            return ScoreSubmissionView.declined(SCORE_ALREADY_POSTED);
        }
        try {
            leaderboardRepository.record(playerName, game.guessesMade());
        } catch (final DataAccessException storeFailure) {
            // Hand the round's one posting slot back so a retry is still possible.
            game.releaseScorePosting();
            throw storeFailure;
        }
        return ScoreSubmissionView.accepted(GameMessages.scorePosted(playerName));
    }
}
