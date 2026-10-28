# Guess the Number with Duke — Java Demo

## Generation metadata

| Field | Value |
|---|---|
| Date of generation | 2026-10-27 |
| Agent harness | IBM Bob |
| Model | claude-opus-4-5 |

## Line counts

Generated with [cloc](https://github.com/AlDanial/cloc). Front-end code (HTML, CSS, JavaScript) and assets (SVG, images) are excluded from both counts so they stay comparable across demos.

### Application code

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            22            100             69            452
Maven                            1             13             11            108
YAML                             1              4              0             36
-------------------------------------------------------------------------------
SUM:                            24            117             80            596
-------------------------------------------------------------------------------
```

### Test code

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             3             61              8            305
YAML                             1              1              0             12
-------------------------------------------------------------------------------
SUM:                             4             62              8            317
-------------------------------------------------------------------------------
```

---

## Stack

- Java 25, Spring Boot 3.5
- Spring Data JPA, PostgreSQL 17
- Springdoc OpenAPI (Swagger UI)
- Micrometer + OpenTelemetry (tracing bridge, logging exporter)
- Testcontainers for integration tests

---

## Prerequisites

- JDK 25+
- Maven 3.9+
- Docker and Docker Compose

---

## Supporting services

Start PostgreSQL:

```bash
# Run from the java-demo-bob-claude-opus-4-5_1/ directory
docker compose up -d
```

Stop and remove the container (data volume is preserved):

```bash
docker compose down
```

### Ports

| Service | Port |
|---|---|
| Spring Boot app | 8080 |
| PostgreSQL | 5432 |

### Database defaults

| Setting | Value |
|---|---|
| Database name | `dukedb` |
| Username | `duke` |
| Password | `duke` |

---

## Schema creation

The schema is generated automatically by Hibernate on first startup (`spring.jpa.hibernate.ddl-auto=update`). No manual SQL step is needed. A fresh database ends up with a `leaderboard_entry` table after the first run.

---

## Build, test, and run

All commands must be run from the `java-demo-bob-claude-opus-4-5_1/` directory.

### Build

```bash
mvn package -DskipTests
```

### Test

Tests provision their own PostgreSQL container via Testcontainers — no manual database setup needed.

> **Docker Desktop 4.x note:** docker-java (used internally by Testcontainers) defaults to requesting Docker API version 1.32, which Docker Desktop 4.x rejects (minimum 1.40). The `pom.xml` passes `-Dapi.version=1.47` to the Surefire JVM to work around this. If tests fail with "Could not find a valid Docker environment", verify Docker is running and that `~/.testcontainers.properties` does not override the strategy to one that cannot reach the daemon.

```bash
mvn test
```

### Run

Start PostgreSQL first (see above), then:

```bash
mvn spring-boot:run
```

Or run the packaged jar:

```bash
java --enable-preview -jar target/java-demo-bob-claude-opus-4-5_1-1.0.0.jar
```

---

## URLs

| Resource | URL |
|---|---|
| Game | http://localhost:8080/ |
| Leaderboard | http://localhost:8080/leaderboard.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator health | http://localhost:8080/actuator/health |

---

## Telemetry

By default the app exports traces and spans to the **console log** via the OpenTelemetry logging exporter. Every API request, game start, guess, and leaderboard operation produces a span. Metrics are collected by Micrometer and can be viewed at `/actuator/metrics`.

To point telemetry at a real OTLP collector, set these properties (e.g. in `application.yaml` or as environment variables):

```yaml
management:
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://your-collector:4318/v1/traces
  otlp:
    metrics:
      export:
        url: http://your-collector:4318/v1/metrics
```

No code change is needed — the destination is purely configuration.

---

## Clearing the leaderboard

The app has no delete endpoint. To clear all scores between runs:

```bash
docker compose exec db psql -U duke -d dukedb -c "TRUNCATE leaderboard_entry RESTART IDENTITY;"
```
