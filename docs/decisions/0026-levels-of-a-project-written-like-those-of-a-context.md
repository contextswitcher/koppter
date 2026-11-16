---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# The Levels of a Project Are Written like Those of a Context

`adr~project-hierarchy~1`

Needs: dsn

## Context and Problem Statement

Projects group the tasks of an outcome, and outcomes have parts: a paper is one of the papers, a topic is one of the topics of research.
The todo.txt format knows a project as one word after `+` and has no notation for a project below another one.
The levels of a context are separated by `>`, with the upper level first ([decision 0017](0017-levels-of-a-context-separated-by-a-greater-than-sign.md)).
How does a task name a project that lies below another project?

## Considered Options

* The upper level first, separated by `>`, such as `+papers>survey`
* The last level first, separated by `<`, such as `+survey<papers`
* A flat project and a hashtag for the upper level, such as `+survey #papers`

## Decision Outcome

Chosen option: "The upper level first, separated by `>`", because it is the notation that contexts already have, so that the user learns one rule and the application has one implementation.

The name that the user thinks of is the last level, and typing the upper levels first would be in the way.
The fields that take a project therefore complete a typed part of a name: `survey` offers `+papers>survey`.

### Consequences

* Good, because a filter, a barrier and a limit for a project include the projects below it, as they do for contexts.
* Good, because a file sorted by other todo.txt tools keeps the projects of one upper project together.
* Bad, because other tools show the whole path and do not know that `+papers` includes `+papers>survey`.
* Bad, because `>` redirects the output in a shell, so that a task added on the command line has to be quoted.
* Bad, because renaming an upper project means changing every task below it.

## Pros and Cons of the Options

### The last level first, separated by `<`

* Good, because the line starts with the name that the user thinks of, and a completion by the start of the name suffices.
* Bad, because projects and contexts would be read in opposite directions.
* Bad, because other tools sort the projects by their last level, which scatters the projects of one upper project.
* Bad, because `<` redirects the input in a shell as well.

### A flat project and a hashtag for the upper level

* Good, because the line needs no new notation and every tool filters by each part.
* Bad, because the hashtag has to be repeated on every task of the project and is forgotten.
* Bad, because the line does not tell that the hashtag is the upper level of the project.
