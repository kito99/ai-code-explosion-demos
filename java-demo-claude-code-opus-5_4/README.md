# Guess the Number with Duke

A web game where Duke, the Java mascot, thinks of a number from 1 to 10 and you
get five guesses to find it. Win and you can post your name to a leaderboard
that ranks wins by how few guesses they took. Built from `BRD.md`.

## Generation

| | |
| --- | --- |
| Date of generation | 2026-10-28 |
| Agent harness | Claude Code 2.1.236 |
| Model | Claude Opus 5 (1M context), model ID `claude-opus-5[1m]` |

### Lines of code

Front-end code (HTML, CSS, JavaScript) and SVG assets are excluded from both
counts, as are Markdown and plain text.

**Application code — 666 lines**, run from this directory:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml compose.yaml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            29            168            111            551
Maven                            1              6              1             78
YAML                             1              2              2             20
Properties                       1              4              0              9
SQL                              1              1              1              8
-------------------------------------------------------------------------------
SUM:                            33            181            115            666
-------------------------------------------------------------------------------
```

**Test code — 762 lines**, run from this directory:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            11            203             17            762
-------------------------------------------------------------------------------
SUM:                            11            203             17            762
-------------------------------------------------------------------------------
```

## Stack

Java 25, Spring Boot 4.1.2 (embedded Tomcat on virtual threads), Spring JDBC,
Flyway, and PostgreSQL 18. The front end is plain HTML, CSS, and JavaScript; the
server owns the secret number, decides which guesses count, and writes every
message the player reads.

## Ports

| Service | Port |
| --- | --- |
| App | 8082 |
| PostgreSQL (on the host) | 5436 |

Other demos in this repository run side by side with this one, so these ports
are specific to this demo.

## Database

The app defaults to these credentials, and `compose.yaml` creates the database
with the same values, so a fresh clone needs no setup:

| | |
| --- | --- |
| Database | `guessduke` |
| User | `guessduke` |
| Password | `guessduke` |

Change them in `src/main/resources/application.properties` and `compose.yaml`
together if you need to.

### Starting and stopping the database

From this directory:

```
docker compose up -d      # start PostgreSQL 18
docker compose stop       # stop it, keeping the data
docker compose down       # stop and remove the container, keeping the data
docker compose down -v    # stop and throw the data away
```

### How the schema gets created

Flyway runs on startup and applies everything under
`src/main/resources/db/migration` to whatever database the app connects to, so a
fresh database ends up with the `leaderboard_entry` table without a manual step.

## Build, test, and run

Every command runs **from this directory** — the repository root holds one
directory per demo and no build file of its own.

```
mvn clean package                    # build and run the full suite
mvn test                             # run the tests only
mvn spring-boot:run                  # run the app on http://localhost:8082
java -jar target/java-demo-claude-code-opus-5_4.jar
```

`mvn spring-boot:run` and the packaged jar both need the database running, so
start it with `docker compose up -d` first.

The tests do not. They start their own PostgreSQL 18 container through
Testcontainers, so the suite runs on a clean machine with nothing but a Docker
daemon available.

## Playing

| Screen | URL |
| --- | --- |
| Game | <http://localhost:8082/> |
| Leaderboard | <http://localhost:8082/leaderboard.html> |

A game starts the moment the page loads, and reloading starts a new one. Duke's
celebration plays once on a win and leaves him back in his resting pose.

## Clearing the leaderboard

The app never deletes entries, so clear them from the database directly. From
this directory, with the database running:

```
docker compose exec postgres psql -U guessduke -d guessduke \
  -c 'TRUNCATE TABLE leaderboard_entry RESTART IDENTITY'
```

`docker compose down -v` also clears it, by discarding the volume along with the
schema. Flyway rebuilds the schema the next time the app starts.

## Duke

The artwork is the official Duke from <https://github.com/openjdk/duke>:
`vector/Thinking.svg` for the Duke who is thinking of the number, and
`vector/Wave.svg` for the Duke who celebrates a win. The waving Duke is inlined
in `index.html` so the raised arm is a CSS animation target of its own.
