package com.example.duke;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Customises the generated OpenAPI definition. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dukeGuessOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Guess the Number with Duke")
                        .description("Server-side API for the Duke guessing game.")
                        .version("1.0.0"));
    }
}
