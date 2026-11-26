---
status: accepted
date: 2026-11-26
decision-makers: Oliver Kopp
---

# Tag for the Time Before Which a Task Is Not Planned

`adr~not-before-tag~1`

Needs: dsn

## Context and Problem Statement

*Postpone* in the notification of a block plans the task later today.
If only the plan records that, the next plan from now on, such as the one after a task is completed, puts the task back to the present.
How does the todo.txt file state that a task is not to be planned before a time of a day?

## Considered Options

* A tag `after:` with a time, which applies on the threshold date of the task
* A time in the threshold date, such as `t:2026-11-26T15:00`
* A tag with a date and a time, such as `notbefore:2026-11-26T15:00`
* No tag: the postponement stays in the plan only

## Decision Outcome

Chosen option: "A tag `after:` with a time, which applies on the threshold date of the task", because `t:` keeps the form that other todo.txt tools read, the time is written like that of `at:` and `dueby:`, and the tag stops having an effect when the day has passed.

### Consequences

* Good, because a postponed task is planned normally again from the next day on, without a change of the file.
* Good, because other tools still hide the task until its threshold date.
* Bad, because the tag stays in the line after its day until the task is completed or edited.
* Bad, because a tag `after:` without `t:` does nothing, which a user who types it may not expect.

## Pros and Cons of the Options

### A time in the threshold date, such as `t:2026-11-26T15:00`

* Good, because one tag holds the moment.
* Bad, because other tools, such as Simpletask, read `t:` as a date and no longer understand it.

### A tag with a date and a time, such as `notbefore:2026-11-26T15:00`

* Good, because it does not depend on `t:`.
* Bad, because it repeats the day that `t:` holds already, and the two can contradict each other.

### No tag: the postponement stays in the plan only

* Good, because the todo.txt file is not changed.
* Bad, because the next plan from now on undoes the postponement.
