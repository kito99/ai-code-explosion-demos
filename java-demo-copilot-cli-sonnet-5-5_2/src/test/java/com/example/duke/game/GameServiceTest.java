package com.example.duke.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class GameServiceTest {

    private static final int SAMPLE_COUNT = 2_000;

    @Test
    void randomSecretCoversTheWholeRangeAndNothingElse() {
        final var generator = new RandomSecretNumberGenerator();

        final var seen = new HashSet<Integer>();
        IntStream.range(0, SAMPLE_COUNT).forEach(_ -> seen.add(generator.nextSecret()));

        assertThat(seen).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void startedGamesStartEmptyAndAreIndependent() {
        final var service = new GameService(new GameStore(), () -> 4);

        final var first = service.startGame();
        final var second = service.startGame();

        assertThat(first.gameId()).isNotEqualTo(second.gameId());
        assertThat(first.guessesMade()).isZero();
        assertThat(first.guessesRemaining()).isEqualTo(GameRules.MAX_GUESSES);
        assertThat(first.status()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void unknownGamesAreReported() {
        final var service = new GameService(new GameStore(), () -> 4);

        assertThatThrownBy(() -> service.guess("missing", 3)).isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void storeEvictsTheOldestGameWhenFull() {
        final var store = new GameStore();
        final var oldest = new Game("oldest", 1);
        store.add(oldest);
        IntStream.range(0, GameStore.MAX_ACTIVE_GAMES).forEach(index -> store.add(new Game("game-" + index, 1)));

        assertThat(store.find("oldest")).isEmpty();
        assertThat(store.find("game-0")).isPresent();
    }
}
