---
status: accepted
date: 2026-11-25
decision-makers: Oliver Kopp
---

# Tag for the Last Day of a Task

`adr~expiry-tag~1`

Needs: dsn

## Context and Problem Statement

Some tasks are pointless once a day has passed: the bins have to be out before the collection, and after it the task is no longer to be done.
Such a task that stays open is planned for the next free time, a week later, and stays overdue until the user removes it.
How does a task state that it is to disappear after a day, and what happens to its line?

## Considered Options

* A tag `exp:` with the last day; the line is deleted after it
* A tag `exp:` with the last day; the task is marked completed with a tag that tells it expired
* A tag `exp:` with the last day; the line is moved to a file `expired.txt` next to todo.txt
* A line that starts with a dash for a task that was dropped

## Decision Outcome

Chosen option: "A tag `exp:` with the last day; the line is deleted after it", because the todo.txt file then holds only what is to be done or was done, other todo.txt tools read it unchanged, and the history of a git repository keeps the deleted line.

A task without a due date is due on its last day, so that the planner plans it before that day.

### Consequences

* Good, because a task without the tag never disappears: the user decides per task.
* Good, because the next occurrence of a recurring task and the entries of `ice_recur.txt` carry the tag like any other.
* Bad, because without git or another history, the removed task is gone without a trace.
* Bad, because other todo.txt tools do not remove the task; they show it as an open task with an unknown tag.

## Pros and Cons of the Options

### A tag `exp:` with the last day; the task is marked completed with a tag that tells it expired

* Good, because the line stays in the file and then in `done.txt`.
* Bad, because other tools and the ranking of a family count the task as done although nobody did it.

### A tag `exp:` with the last day; the line is moved to a file `expired.txt` next to todo.txt

* Good, because the removed tasks stay readable without a history.
* Bad, because it is another file next to todo.txt that has to be merged and that no other tool knows.

### A line that starts with a dash for a task that was dropped

* Bad, because the todo.txt format knows only `x` as a mark: other tools read such a line as an open task whose description starts with a dash.
