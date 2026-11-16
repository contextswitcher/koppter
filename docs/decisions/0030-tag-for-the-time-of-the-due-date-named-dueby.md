---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# The Tag for the Time of the Due Date Is Named `dueby:`

`adr~due-time-tag-name~1`

Needs: dsn

## Context and Problem Statement

[Decision 0022](0022-tag-for-the-time-by-which-a-task-is-due.md) states the time by which a task is due in a tag next to the due date and names that tag `by:`.
The tag counts together with a due date only, which its name does not tell.
Since [decision 0028](0028-tag-for-the-deadline-next-to-the-due-date.md), a task has a second date, the deadline, and a reader cannot tell to which of the two dates `by:` belongs.
What is the name of the tag?

## Considered Options

* `dueby:14:00`
* `by:14:00`, as before
* `dueT:14:00`
* The time as part of the due date, `due:2026-11-16T14:00`

## Decision Outcome

Chosen option: "`dueby:14:00`", because the name tells that the time belongs to the due date, and the due date stays a date that other programs read.

This record supersedes decision 0022 in the name of the tag; its reasons for a tag next to the due date hold.
The application is young enough to drop the old name: `by:` is not read anymore.

### Consequences

* Good, because `due:2026-11-16 dueby:14:00` reads as one statement, and the deadline has its time in its own value.
* Good, because a program that does not know the tag still shows the task as due on that day.
* Bad, because a file that has `by:` loses the time until the tag is renamed in it.
* Bad, because the name is longer to type.

## Pros and Cons of the Options

### `by:14:00`, as before

* Good, because nothing changes, and the tag is short.
* Bad, because the name does not tell that the tag needs a due date, nor to which date it belongs.

### `dueT:14:00`

* Good, because it recalls the separator of date and time in ISO 8601.
* Bad, because it would be the only tag with a capital letter.

### The time as part of the due date, `due:2026-11-16T14:00`

* Good, because date and time are one value, as for the deadline.
* Bad, because programs that read `due:` as a date do not read that value: the task loses its due date there.
