---
status: accepted
date: 2026-11-20
decision-makers: Oliver Kopp
---

# Weeks Start on Monday

`adr~week-starts-on-monday~1`

Needs: impl

## Context and Problem Statement

The agenda, the week view, the time maps, the budgets of the planner, the dates typed in words and the week numbers of `calendar.txt` count a week from Monday to Sunday.
The calendar of Material, in which a date of a task is chosen, takes the first day of its weeks from the locale of the system and starts them on Sunday for a locale such as that of the United States.
On which day does a week start, and does the user choose that day?

## Considered Options

* Monday everywhere
* The first day of the locale of the system
* A setting for the first day

## Decision Outcome

Chosen option: "Monday everywhere", because the rest of the application counts that way already, as ISO 8601 does, and one rule for all views needs neither a setting nor code that passes it along.

The calendar of a date gets the locale of the system with Monday as its first day of the week, so the names of the months and the days stay in the language of the system.
The intervals of `ice_recur.txt` keep counting their weeks from Sunday, as the add-on does that defines the format.

### Consequences

* Good, because a week looks the same in the calendar of a date as in the week view and the time maps.
* Good, because a weekend, which stands for its Sunday, is the end of a row of the calendar.
* Bad, because a user who is used to weeks from Sunday cannot have them.
* Bad, because the first day reaches the calendar of Material as a keyword of the locale, which an Android before version 14 may ignore; the calendar then starts its weeks as the system does.

## Pros and Cons of the Options

### The first day of the locale of the system

* Good, because the calendar of a date looks as in other applications of the device.
* Bad, because the week view, the budgets and the dates typed in words either differ from that calendar or have to follow the locale as well, and a plan would then depend on the device that computes it.

### A setting for the first day

* Good, because each user gets the week they are used to.
* Bad, because every place that counts a week has to read the setting: the planner, the time maps, the dates in words and the views.
* Bad, because the plan and `calendar.txt` are shared between devices through the file, so the setting would have to be part of the file to give the same plan everywhere.
