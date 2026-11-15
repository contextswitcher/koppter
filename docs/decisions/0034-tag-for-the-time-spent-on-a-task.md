---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# Tag for the Time Spent on a Task

`adr~time-spent-tag~1`

Needs: dsn

## Context and Problem Statement

The plan gives each task the time of its tag `dur:`.
A task that was worked on for a part of that time is planned with all of it again, unless the user lowers the duration by hand.
How does the plan learn what is left of a task?

## Considered Options

* A tag `spent:` on the task
* Lowering `dur:` by hand
* A log of the times worked, with start and end

## Decision Outcome

Chosen option: "A tag `spent:` on the task", because the plan needs one number per task, and the estimate stays next to it.

The plan gives the task its duration without the time spent.
A task that took its duration and is still open gets the default duration.

### Consequences

* Good, because a completed task tells its estimate and the time it took.
* Good, because the tag is written by hand, by the edit dialog or by one click on a block of the agenda, without a clock that runs.
* Bad, because other programs for the format do not know the tag: they show it and plan nothing by it.
* Bad, because the time is a sum: the file does not tell on which days it was worked.

## Pros and Cons of the Options

### Lowering `dur:` by hand

* Good, because nothing is added to the notation.
* Bad, because the estimate is lost from the line, so that it cannot be compared with the time the task took.
* Bad, because the rest has to be worked out by the user.

### A log of the times worked, with start and end

* Good, because it tells when it was worked, which reports and invoices need.
* Bad, because it needs a clock that is started and stopped, and the corrections of a clock that was forgotten.
* Bad, because the plan does not need it: it reads the sum only.
