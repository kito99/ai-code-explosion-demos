# java-demo-bob-claude-opus-4-5_5

Guess the Number with Duke — Java demo built by Bob (IBM Bob agent harness) using claude-opus-4-5.

## Generation details

| Field | Value |
|---|---|
| Date of generation | 2026-10-28 |
| Agent harness | Bob (IBM Bob) |
| Model | claude-opus-4-5 |

## Lines of code

Commands run from the project directory (`java-demo-bob-claude-opus-4-5_5/`).

**Application code** (implementation + build/config, excluding HTML/CSS/JS/SVG/Markdown/Text):

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             8             44             44            244
Maven                            1              6              1             44
Properties                       1              0              0              2
-------------------------------------------------------------------------------
SUM:                            10             50             45            290
-------------------------------------------------------------------------------
```

**Test code:**

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             3             82             11            268
-------------------------------------------------------------------------------
SUM:                             3             82             11            268
-------------------------------------------------------------------------------
```

## Ports

| Service | Port |
|---|---|
| App (HTTP) | 8080 |

Nothing else needs to be running. The app uses an in-memory leaderboard — no external database.

## URLs

| Screen | URL |
|---|---|
| Game | http://localhost:8080/ |
| Leaderboard | http://localhost:8080/leaderboard |

## Clearing the leaderboard

The app has no delete endpoint. Restart the server to reset all scores.

## Build, test, and run

All commands run from the `java-demo-bob-claude-opus-4-5_5/` directory.

**Build:**
```bash
mvn package -q
```

**Test:**
```bash
mvn test
```

**Run:**
```bash
mvn spring-boot:run
```

or run the packaged jar:
```bash
java -jar target/java-demo-bob-claude-opus-4-5_5.jar
```

Stop with `Ctrl+C`.
