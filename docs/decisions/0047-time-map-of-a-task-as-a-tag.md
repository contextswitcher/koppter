---
status: accepted
date: 2026-11-20
decision-makers: Oliver Kopp
---

# Time Map of a Task as a Tag

`adr~task-time-map~1`

Needs: dsn

## Context and Problem Statement

The plan takes the time map of the first context of a task that has one.
SkedPal gives each task its time map, and a single task, such as a paper to read in the evening, may belong into other times than the rest of its context.
How does a task name a time map of its own?

## Considered Options

* A tag `map:` with the name of the time map
* A context of its own below the context, with a time map in `koppter.yml`
* A list of tasks and their time maps in `koppter.yml`

## Decision Outcome

Chosen option: "A tag `map:` with the name of the time map", because the choice stays on the line of the task, which every device and every editor of todo.txt shows and changes.

### Consequences

* Good, because a task is given its times when it is written, on Android as well, without changing `koppter.yml`.
* Good, because the contexts keep their meaning: the kind of work or the place, not the times of one task.
* Bad, because a name of a time map cannot hold a space in a tag.
* Bad, because a renamed time map leaves the tags with the old name; such a task falls back to the map of its context.

## Pros and Cons of the Options

### A context of its own below the context, with a time map in `koppter.yml`

* Good, because it works already and needs no new notation.
* Bad, because a context made for the times of one task fills the board and the filters.

### A list of tasks and their time maps in `koppter.yml`

* Good, because the line of the task stays short.
* Bad, because the list names tasks by their text, which changes when a task is edited.
