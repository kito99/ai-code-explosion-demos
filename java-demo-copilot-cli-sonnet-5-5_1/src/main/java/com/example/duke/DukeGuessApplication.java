package com.example.duke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DukeGuessApplication {

    static void main(final String[] arguments) {
        SpringApplication.run(DukeGuessApplication.class, arguments);
    }
}
