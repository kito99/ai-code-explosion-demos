package com.example.duke.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Describes the API the front-end calls, served at `/v3/api-docs` and browsable at `/swagger-ui.html`.
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    public OpenAPI dukeGuessOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Guess the Number with Duke")
                .version("1.0.0")
                .description("""
                        Server-side actions behind the game: start a round, submit a guess, post a win \
                        to the leaderboard, and read the leaderboard.""")
                .license(new License().name("Demo")));
    }
}
