---
status: accepted
date: 2026-11-21
decision-makers: Oliver Kopp
---

# Weeks of ice_recur Start on Monday

`adr~weeks-of-ice-recur-start-on-monday~1`

Needs: impl

## Context and Problem Statement

An entry of `ice_recur.txt` with an interval of weeks, such as `weekly 2 ; day Friday, Sunday`, appears in every second week.
[Decision 0049](0049-weeks-start-on-monday.md) lets a week start on Monday everywhere in the application and left these intervals out: they counted their weeks from Sunday, the default of the library with which the add-on computes its schedules.
From which day do the weeks of such an interval count?

## Considered Options

* Monday, as everywhere in the application
* Sunday, as the add-on counts
* A week start per entry

## Decision Outcome

Chosen option: "Monday, as everywhere in the application", because an entry with a Friday and a Sunday then gives the two days of one weekend, as the week view shows them, and the application has one rule for a week.

This replaces the exception that decision 0049 states for these intervals.
An entry without an interval, or with an interval of one, appears on the same days as before.

### Consequences

* Good, because the schedule dialog and the week view agree on what a week is.
* Bad, because the Sundays of an existing entry with an interval above one move by a week.
* Bad, because the add-on, run on the same file, adds the task of such an entry on other Sundays than the application.

## Pros and Cons of the Options

### Sunday, as the add-on counts

* Good, because the application and the add-on add the same tasks from the same file.
* Bad, because a Sunday belongs to the week of the Monday after it, unlike in every other view.

### A week start per entry

* Good, because each entry states how it counts.
* Bad, because the add-on does not know such a specifier and may reject the line.
