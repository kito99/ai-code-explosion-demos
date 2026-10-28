# Shared demo instructions

These instructions are the same for every demo in this repository. They add to
the prompt that sent you here, which supplies the project name prefix to use.

## Existing demos

Build the app from `BRD.md` and the instructions in these prompts alone. Do not
read or copy from the demos already in this repository, neither their source nor
their configuration nor their READMEs, so each demo shows what this harness and
model produce on their own. `AGENTS.md` is the exception, since it carries the
coding conventions every demo follows.

Reading the names of the existing demo folders is fine and necessary to work
out this demo's own name below.

## Project name

The name of the project should be `[prefix]-demo-[agent harness]-[model]_xx`,
using the prefix your prompt gave you, where `xx` is the suffix of the last demo
with the same prefix, agent harness, and model, plus 1. An underscore separates
that suffix, so it stays readable against model names that already carry
hyphens. Name the harness and the model the way the existing demo folders do, so
Claude Code running Opus 5 gives `claude-code` and `opus-5`. For example, if the
most recent matching demo is `java-demo-claude-code-opus-5_1`, the new folder is
`java-demo-claude-code-opus-5_2`. If no demo exists with the same prefix, agent
harness, and model, use the suffix `1`.

Use that same folder name as the build artifact id, so the output of a build
says which demo produced it. Carry it through everything derived from the
artifact id as well, such as the name of the packaged file. Leave Java package
names out of this, since a folder name containing hyphens and a digit is not a
legal package; `com.example` and a short app-specific segment is fine there.

## Tests

The test suite must run on a clean machine with nothing started by hand, so
tests provision whatever they need themselves.

## Files to copy

When you are finished, copy the following into the new folder, so we can keep
track of the exact versions used for the app:

* `BRD.md`
* `AGENTS.md`
* The prompt file you were given
* This file

## README.md

In the `README.md` file, record the following:

* Date of generation
* Name of the agent harness
* Name and version of the model
* number of lines of code, as two separate counts:
  * Application code: the implementation language plus build and configuration
    files
  * Test code: the same, for the test sources

  This MUST be generated using cloc. Leave front-end code (HTML, CSS,
  JavaScript) and assets such as SVG images out of both counts. Record the
  commands used and their output tables so the numbers can be reproduced.
  Adjust the paths below to the project's own layout but keep the language
  exclusions so counts stay comparable across demos:
  ```
  cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
  cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
  ```

Also record:

* The ports the app listens on
* Anything else that has to be running, and how to start and stop it
* The URL for the leaderboard screen
* How to clear the leaderboard between runs, since the app itself has no way to
  delete entries

Also record how to build, test, and run the app. Every command must state the
directory it runs from: this repository holds one directory per demo and no
build file of its own, so commands run from the root will fail.
