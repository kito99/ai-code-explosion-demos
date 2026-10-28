package com.example.duke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/// Boots the "Guess the Number with Duke" web application.
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class DukeGuessApplication {

    public static void main(final String[] args) {
        SpringApplication.run(DukeGuessApplication.class, args);
    }
}
