---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# The Time of a Deadline Is Part of Its Date

`adr~deadline-time~1`

Needs: dsn

## Context and Problem Statement

A few deadlines are a time of a day: a submission closes at noon.
The time of the due date has its own tag, `by:`, so that `due:` stays a date that other programs read.
The tag `deadline:` is read by this application only.
How does a todo.txt line state the time of a deadline?

## Considered Options

* The time as part of the value, `deadline:2026-11-30T12:00`
* A second tag for the time, as `by:` is for the due date
* The tag `by:` for the time of the deadline as well

## Decision Outcome

Chosen option: "The time as part of the value, `deadline:2026-11-30T12:00`", because date and time are one value that cannot fall apart, and no other program loses anything by it.

The value is a local date and time of ISO 8601 without seconds and without an offset, as the other times of a task are local times.
A task without a due date is due on its deadline, and by the time of the deadline.

### Consequences

* Good, because the common case stays a date: `deadline:2026-11-30`.
* Good, because the tags of the due date are not touched: `due:` stays a date, and `by:` its time.
* Bad, because the times of the two dates are written in two ways.
* Bad, because the value is no date-time of RFC 3339, which demands seconds and an offset.

## Pros and Cons of the Options

### A second tag for the time, as `by:` is for the due date

* Good, because both dates are written in the same way.
* Bad, because a further tag has to be known and typed, for a case that is rare.
* Bad, because the deadline is then spread over two tags, and one can be changed without the other.

### The tag `by:` for the time of the deadline as well

* Good, because no further notation is needed.
* Bad, because a task with a due date and a deadline then has one time for two dates.
