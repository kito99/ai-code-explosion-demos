package com.example.duke.web;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OpenAPI openApiDefinition() {
        return new OpenAPI().info(new Info()
                .title("Guess the Number with Duke")
                .version("1.0.0")
                .description("Game and leaderboard actions behind the Duke guessing game."));
    }
}
