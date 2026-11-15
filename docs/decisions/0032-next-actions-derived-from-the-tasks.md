---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# Next Actions Are Derived From the Tasks

`adr~next-actions-derived~1`

Needs: dsn

## Context and Problem Statement

Getting Things Done works from the next action of each project, and a project without one is stuck.
The todo.txt format has no notation for a next action.
How does the application know the next action of a project?

## Considered Options

* Derived: the first task of the project that can be worked on
* A mark on the task, such as the hashtag `#next` or the context `@next`
* The priority `A`

## Decision Outcome

Chosen option: "Derived: the first task of the project that can be worked on", because the file already tells what cannot be worked on, and a mark would have to be moved by hand after each completed task.

A task cannot be worked on if it has the context `@waiting`, the hashtag `#someday`, a threshold date in the future, or waits for another open task through `id:` and `p:`.
The first of the remaining tasks is the first in the order of the list, so priority and due date decide.

### Consequences

* Good, because completing a task makes the next one of its project the next action without an edit.
* Good, because the file stays as other programs read it: nothing is written for it.
* Bad, because `@waiting` and `#someday` now have a meaning in the application, which a file that uses the names otherwise does not intend.
* Bad, because the order of the steps of a project is known only as far as priorities, due dates and dependencies tell it.
* Bad, because a project whose tasks are all completed is not known as a project anymore and is not reported as stuck.

## Pros and Cons of the Options

### A mark on the task, such as the hashtag `#next` or the context `@next`

* Good, because the user states the next action, whatever the order of the list is.
* Bad, because each completed next action needs a second edit that marks its successor, which is forgotten.
* Bad, because a project without the mark is stuck by omission, not by the state of its tasks.

### The priority `A`

* Good, because every program for the format shows it.
* Bad, because the priority then no longer tells the importance, and the list order is led by the next actions of all projects.
