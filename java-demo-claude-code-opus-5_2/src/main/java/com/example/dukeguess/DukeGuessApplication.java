package com.example.dukeguess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/// Boots the Guess the Number with Duke web game.
@SpringBootApplication
@ConfigurationPropertiesScan
public class DukeGuessApplication {

    public static void main(final String[] args) {
        SpringApplication.run(DukeGuessApplication.class, args);
    }
}
