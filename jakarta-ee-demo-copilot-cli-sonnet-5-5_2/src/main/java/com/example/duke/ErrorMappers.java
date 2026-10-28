package com.example.duke;

import com.example.duke.Dtos.ErrorResponse;

import jakarta.json.bind.JsonbException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

public final class ErrorMappers {

    private ErrorMappers() {
    }

    private static Response error(int status, String message) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(new ErrorResponse(message)).build();
    }

    @Provider
    public static class Validation implements ExceptionMapper<ConstraintViolationException> {
        @Override
        public Response toResponse(ConstraintViolationException e) {
            String message = e.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .findFirst()
                    .orElse("That doesn't look right. Please try again.");
            return error(400, message);
        }
    }

    @Provider
    public static class Game implements ExceptionMapper<GameException> {
        @Override
        public Response toResponse(GameException e) {
            return error(e.status(), e.getMessage());
        }
    }

    @Provider
    public static class MalformedBody implements ExceptionMapper<JsonbException> {
        @Override
        public Response toResponse(JsonbException e) {
            return error(400, "That doesn't look right. Please try again.");
        }
    }

    @Provider
    public static class BadRequest implements ExceptionMapper<BadRequestException> {
        @Override
        public Response toResponse(BadRequestException e) {
            return error(400, "That doesn't look right. Please try again.");
        }
    }
}
