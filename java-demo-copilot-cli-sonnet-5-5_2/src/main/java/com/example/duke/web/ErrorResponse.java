package com.example.duke.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error details suitable for showing to the player.")
public record ErrorResponse(@Schema(example = "Please enter a whole number from 1 to 10.") String message) {
}
