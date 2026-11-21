---
status: accepted
date: 2026-11-21
decision-makers: Oliver Kopp
---

# Time Log next to the todo.txt File

`adr~time-log-file~1`

Needs: dsn

## Context and Problem Statement

The clock of a task adds its time to `spent:` ([decision 0044](0044-clock-of-a-task-as-a-tag-with-its-start.md)), which keeps the sum only.
The user wants to see on which days and at which times they worked on what, and when they were away, as the status tracker of SkedPal shows it.
Where are these times kept?

## Considered Options

* A file `timelog.txt` next to todo.txt with one line per time
* The times as tags on the task, such as `log:2026-11-21T16:50/30m`
* The times in `calendar.txt` beside the plan

## Decision Outcome

Chosen option: "A file `timelog.txt` next to todo.txt with one line per time", because it holds times that belong to no task, such as the time away, and lines that are only appended merge without conflict.

The clock stays on the task as `started:`, and `spent:` stays the sum that the plan reads; the log is written in addition.

### Consequences

* Good, because the file is plain text in the layout of `calendar.txt` and can be read, summed and corrected with any editor.
* Good, because the time away is in the file as well, so that another device shows it.
* Bad, because a line names its task by text: after the task is renamed, older lines keep the old name.
* Bad, because a correction of `spent:` in the edit dialog does not change the log, so the two can differ.

## Pros and Cons of the Options

### The times as tags on the task

* Good, because the times stay with their task, also after it is renamed.
* Bad, because the line of a task that is worked on often grows without end.
* Bad, because the time away belongs to no task.

### The times in `calendar.txt` beside the plan

* Good, because the times and the plan of a day are in one place.
* Bad, because each plan replaces the days from today on, and with them the times of today.
* Bad, because a merge of two devices takes the plan made last as a whole.
