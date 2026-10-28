package com.example.duke.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    /// A database that is down fails when the transaction opens; one that fails mid-query does not.
    static Stream<RuntimeException> databaseFailures() {
        return Stream.of(new CannotCreateTransactionException("could not open a connection"),
                new DataAccessResourceFailureException("the query failed"));
    }

    @ParameterizedTest
    @MethodSource("databaseFailures")
    void reportsAnUnreachableDatabaseAsTemporary(final RuntimeException failure) {
        final var problem = handler.onDatabaseUnreachable(failure);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getDetail()).contains("try posting your score again");
    }
}
