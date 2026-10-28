package com.example.duke.api.dto;

import com.example.duke.error.ErrorKind;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/// What a failed request returns. The message is written for the player, and the kind is
/// there so a client can tell the failures apart without parsing text.
@Schema(name = "Error", description = "A request that could not be carried out")
public record ErrorResponse(

        @Schema(description = "Which kind of failure this is")
        ErrorKind kind,

        @Schema(description = "A friendly explanation to show the player")
        String message) {
}
