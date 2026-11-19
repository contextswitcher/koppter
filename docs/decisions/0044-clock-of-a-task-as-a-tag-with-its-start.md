---
status: accepted
date: 2026-11-19
decision-makers: Oliver Kopp
---

# Clock of a Task as a Tag with Its Start

`adr~clock-tag~1`

Needs: dsn

## Context and Problem Statement

The tag `spent:` holds the time worked on a task ([decision 0034](0034-tag-for-the-time-spent-on-a-task.md)).
It is written by hand or by a button of a block of the agenda, which assumes that the work took the block.
Tools such as the status tracker of SkedPal start a clock when the work begins and count the time until it stops.
Where does koppter keep a clock that runs, so that it survives a restart and is seen on another device?

## Considered Options

* A tag `started:` with the start on the task
* A log file next to todo.txt with the start and end of each time worked
* The start in the settings of the device

## Decision Outcome

Chosen option: "A tag `started:` with the start on the task", because the time ends up in `spent:` anyway, and the line of the task is synchronized with it.

### Consequences

* Good, because the clock runs on in the file: a restart, another device or a synchronization through git shows it.
* Good, because stopping it writes the sum that the plan already reads, without a second store.
* Bad, because the file does not tell on which days and at which times the task was worked, as with `spent:` alone.
* Bad, because starting and stopping each change the line, so that each is a commit in a git repository.

## Pros and Cons of the Options

### A log file next to todo.txt with the start and end of each time worked

* Good, because it tells when it was worked, which reports and invoices need, and holds activities that are no task.
* Bad, because it is a second store whose lines name tasks by their text, which changes when a task is edited.
* Bad, because the plan reads the sum only, which would be computed from the log or kept twice.

### The start in the settings of the device

* Good, because the todo.txt file does not change while the clock runs.
* Bad, because another device does not see the clock, and a clock started there is stopped nowhere else.
