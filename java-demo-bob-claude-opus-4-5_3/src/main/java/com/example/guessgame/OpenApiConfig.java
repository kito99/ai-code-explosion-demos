package com.example.guessgame;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI guessGameOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Guess the Number with Duke API")
                        .version("1.0")
                        .description("Server-side game API for the Duke number-guessing game demo.")
                );
    }
}
