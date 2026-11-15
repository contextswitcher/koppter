---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# A Tag States the Deadline Next to the Due Date

`adr~deadline-tag~1`

Needs: dsn

## Context and Problem Statement

Some tasks have two dates: the day that someone else set, and an earlier day by which the user wants to be done, to keep a buffer.
After a missed deadline, the work is planned anew, and the day that was set should still be on record.
The todo.txt format has one due date.
How does a todo.txt line state both dates?

## Considered Options

* A tag `deadline:` for the day that others set, next to `due:` as the day to be done by
* `due:` as the day that others set, and a new tag for the day to be done by
* The day that others set as text in the description
* The threshold date `t:` and the due date as the two dates

## Decision Outcome

Chosen option: "A tag `deadline:` for the day that others set, next to `due:` as the day to be done by", because `due:` is the date that other programs order the tasks by and that the application moves, and that is the date the user acts on.

A task without `due:` is due on its `deadline:`.
The application does not move the deadline, except to the next occurrence of a recurring task.

### Consequences

* Good, because a task without the tag is read as before.
* Good, because a program that does not know the tag orders the tasks by the day the user wants to be done.
* Good, because the days between the two dates are known as a buffer: a task in them is due, and overdue only after the deadline.
* Bad, because no other todo.txt program is known to read the tag: there, a task with a deadline only has no due date.
* Bad, because `due:` has two meanings: the day that others set for a task without the tag, and the day to be done by for a task with it.

## Pros and Cons of the Options

### `due:` as the day that others set, and a new tag for the day to be done by

* Good, because `due:` keeps one meaning.
* Bad, because the list order, the menu that moves the due date, the dates typed in words and the planner would have to use the new tag where a task has it.
* Bad, because other programs order the tasks by the day that others set, not by the day the user acts on.

### The day that others set as text in the description

* Good, because it needs nothing new.
* Bad, because the application cannot tell when that day has passed.

### The threshold date `t:` and the due date as the two dates

* Good, because both tags exist.
* Bad, because the threshold date is the day from which the task is worked on, not a day by which it is done.
