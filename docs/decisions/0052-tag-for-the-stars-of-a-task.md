---
status: accepted
date: 2026-11-21
decision-makers: Oliver Kopp
---

# Tag for the Stars of a Task

`adr~stars-tag~1`

Needs: dsn

## Context and Problem Statement

In a family, a member who completes a task gets stars for it, and a ranking compares the stars of the members over a day, a week, a month, a quarter and a year.
Tasks are worth different numbers of stars.
Where is the worth of a task kept, and where the stars that a member has?

## Considered Options

* A tag `stars:` on the task, the ranking derived from the completed tasks
* A file next to todo.txt that logs each star with its day and its member
* The priority of the task as its worth

## Decision Outcome

Chosen option: "A tag `stars:` on the task, the ranking derived from the completed tasks", because the todo.txt file stays the only place that holds the tasks and what came of them, and a tag is kept by other todo.txt tools.

A task without the tag is worth one star.
The stars of a completed task go to the member in whose list it is, on the day of its completion date, which is therefore written for the task of a member whatever the settings say about dates.

### Consequences

* Good, because reopening a task takes its stars back without a second file to correct.
* Good, because the next occurrence of a recurring task is worth the same, as it keeps its tags.
* Bad, because a completed task that is deleted or moved to another file no longer counts: the file has to keep the completed tasks for as long as the ranking looks back, which is a year.
* Bad, because the stars go to the member of the list and not to the person who ticked the task off, whom the application does not know.

## Pros and Cons of the Options

### A file next to todo.txt that logs each star with its day and its member

* Good, because the stars stay when the completed tasks are removed from the todo.txt file.
* Bad, because a task that is reopened, edited or moved to another member leaves a line that has to be found and corrected.
* Bad, because it is a second file to merge, and the two files can contradict each other.

### The priority of the task as its worth

* Good, because it needs no notation of its own.
* Bad, because the priority orders the list and the plan: a task that is worth much would stand first whether it is urgent or not.
* Bad, because a completed task has no priority in the todo.txt format.
