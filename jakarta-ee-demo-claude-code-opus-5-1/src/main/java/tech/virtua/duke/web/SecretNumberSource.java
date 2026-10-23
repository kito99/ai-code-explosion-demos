package tech.virtua.duke.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import tech.virtua.duke.game.RandomSecretNumberPicker;
import tech.virtua.duke.game.SecretNumberPicker;

/// Hands the application its secret numbers. Keeping the producer here leaves the game
/// package free of container annotations, so the rules test without a container.
@ApplicationScoped
public class SecretNumberSource {

    @Produces
    @ApplicationScoped
    public SecretNumberPicker secretNumberPicker() {
        return new RandomSecretNumberPicker();
    }
}
