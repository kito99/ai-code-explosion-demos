package com.example.dukeguess.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/// Runs a test against the whole app on a random port, with a containerised database and
/// a secret number the test chooses.
///
/// Every integration test shares this one combination, so Spring keeps a single context
/// and the database container starts once for the whole suite.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresContainerConfiguration.class, FixedSecretNumberConfiguration.class,
        MetricsCaptureConfiguration.class})
public @interface DukeGuessIntegrationTest {
}
