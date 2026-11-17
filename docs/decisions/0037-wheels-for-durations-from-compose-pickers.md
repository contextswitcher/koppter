---
status: accepted
date: 2026-11-17
decision-makers: Oliver Kopp
---

# Wheels for Durations From Compose Pickers

`adr~wheel-for-durations-of-compose-pickers~1`

Needs: impl

## Context and Problem Statement

The dialog of a task has fields for the duration, the travel time and the time spent, which take a text such as `1h30m`.
A date is chosen in the calendar of Material and a time on its clock face, but Material has nothing to choose a duration with.
How does the user choose a duration without typing it?

## Considered Options

* The wheels of the library Compose Pickers
* A menu of usual durations
* The clock face of Material
* Wheels written in the application

## Decision Outcome

Chosen option: "The wheels of the library Compose Pickers", because a wheel for the hours and one for the minutes show a duration as what it is and are known from the timers of phones, and the library has them for Android and the desktop.

A button in each of the three fields opens a dialog with the two wheels; the minutes have steps of five.
The fields still take typed text, which is the way to any other duration.

### Consequences

* Good, because a duration is chosen with two movements on a phone.
* Good, because the wheels take their colors and their font from the theme of the application.
* Bad, because the application depends on a library that has not reached version 1.0, announces a changed interface for its next version and has few users.
* Bad, because the library is built for an earlier version of Compose Multiplatform than the application uses and brings the resources component of Compose along.
* Bad, because a wheel is slower with a mouse than a typed text.

## Pros and Cons of the Options

### A menu of usual durations

* Good, because it needs no library and one click chooses a duration.
* Bad, because it offers a fixed list, and a duration that is not in it is typed.

### The clock face of Material

* Good, because it is part of Material, which the application uses already.
* Bad, because it shows times of a day: a duration of two hours would be chosen as two o'clock.

### Wheels written in the application

* Good, because it needs no library.
* Bad, because a wheel that snaps, scrolls with the mouse and is read by a screen reader is code that the library has already.
