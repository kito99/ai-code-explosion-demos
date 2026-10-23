package tech.virtua.duke;

import java.util.concurrent.ThreadLocalRandom;

import static tech.virtua.duke.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.GameRules.LOWEST_NUMBER;

/// Chooses the number Duke is thinking of.
@FunctionalInterface
public interface SecretNumberPicker {

    /// A number inside the guessable range.
    int pickSecretNumber();

    /// A picker that draws every number in range with equal likelihood, and
    /// draws independently each time it is called.
    static SecretNumberPicker random() {
        return () -> ThreadLocalRandom.current().nextInt(LOWEST_NUMBER, HIGHEST_NUMBER + 1);
    }
}
