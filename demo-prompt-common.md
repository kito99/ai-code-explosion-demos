# Shared demo instructions

These instructions are the same for every demo in this repository. The prompt
that sent you here names the application stack to build and the project name
prefix to use; everything else is below.

## Project name

The name of the project should be `[prefix]-demo-[agent harness]-[model]-xx`,
using the prefix your prompt gave you, where `xx` is the suffix of the last demo
with the same prefix, agent harness, and model, plus 1. For example, if the most
recent matching demo is `java-demo-claude-opus-1`, the new folder is
`java-demo-claude-opus-2`. If no demo exists with the same prefix, agent harness,
and model, use the suffix `1`.

## Database

Use PostgreSQL 18 from the official Docker image. Pin the version rather than
tracking the latest, so demos built months apart stay comparable.

Commit a `compose.yaml` in the project folder so the database starts with a
single `docker compose up -d`. Give the database name, user, and password the
values the app already defaults to, so a fresh clone runs with no setup.

How the schema gets created is up to the stack. Generating it from mapped
classes is fine where the stack offers that; a committed `schema.sql` or a
migration tool is fine where it does not. Either way, a fresh database must end
up with the right tables without a manual step, and the README must say which
approach the app took.

## Telemetry

Export telemetry to the console by default, so the app needs nothing else
running. Spans and metrics land in the app's own log, which is enough to show
that the telemetry is there.

Do not add a collector to `compose.yaml`. NFR-6 only requires that the
destination be configurable, so record in the README which property to change,
and the OTLP endpoint to set, for pointing the app at a real collector later.

Use whatever OpenTelemetry support the stack provides rather than wiring up an
exporter by hand.

## Tests

The test suite must run on a clean machine without anyone starting a database by
hand, so tests provision their own. Use Testcontainers or the equivalent the
stack provides.

## Files to copy

When you are finished, copy the following into the new folder, so we can keep
track of the exact versions used for the app:

* `BRD.md`
* The prompt file you were given
* This file

## README.md

In the `README.md` file, record the following:

* Date of generation
* Name of the agent harness
* Name and version of the model
* # of lines of code, as two separate counts:
  * Application code: the implementation language plus build and configuration
    files
  * Test code: the same, for the test sources

  This MUST be generated using cloc. Leave front-end code (HTML, CSS,
  JavaScript) and assets such as SVG images out of both counts. Record the
  commands used and their output tables so the numbers can be reproduced.
  Adjust the paths below to the project's own layout, but keep the language
  exclusions so counts stay comparable across demos:

      cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
      cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test

Also record:

* How to start and stop the supporting services
* The ports the app and the database listen on
* The database name, user, and password the app defaults to
* How the schema gets created
* The URLs for the OpenAPI documentation and the leaderboard screen
* How to view the telemetry the app emits
* How to clear the leaderboard between runs, since the app itself has no way to
  delete entries

Also record how to build, test, and run the app. Every command must state the
directory it runs from: this repository holds one directory per demo and no
build file of its own, so commands run from the root will fail.
