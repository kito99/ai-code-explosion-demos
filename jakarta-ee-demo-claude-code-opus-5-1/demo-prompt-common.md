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

Also record how to build, test, and run the app. Every command must state the
directory it runs from: this repository holds one directory per demo and no
build file of its own, so commands run from the root will fail.
