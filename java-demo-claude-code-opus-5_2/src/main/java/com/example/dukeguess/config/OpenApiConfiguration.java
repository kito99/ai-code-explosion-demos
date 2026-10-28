package com.example.dukeguess.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Describes the API the browser talks to, served as an OpenAPI definition and as
/// Swagger UI.
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    private static final String DESCRIPTION = """
            Server-side actions behind Guess the Number with Duke. The server holds the \
            secret number and the guess count for each round, so a posted score always \
            reflects the game the server scored.
            """;

    @Bean
    OpenAPI dukeGuessOpenApi(@Value("${spring.application.name}") final String applicationName) {
        return new OpenAPI().info(new Info()
                .title("Guess the Number with Duke")
                .version("1.0.0")
                .description(DESCRIPTION + "Built by the " + applicationName + " demo."));
    }
}
