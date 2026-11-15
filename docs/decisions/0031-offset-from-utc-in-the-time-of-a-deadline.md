---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# The Time of a Deadline May State Its Offset From UTC

`adr~deadline-offset~1`

Needs: dsn

## Context and Problem Statement

A deadline that others set is often given for another place: a conference closes its submission at the end of a day "anywhere on Earth", which is twelve hours behind UTC.
[Decision 0029](0029-time-of-a-deadline-as-part-of-its-date.md) writes the time of a deadline as a local time.
How does a todo.txt line state a deadline as it was announced?

## Considered Options

* An offset from UTC after the time, `deadline:2026-11-30T23:59-12:00`
* The name of a time zone after the time, such as `Europe/Berlin`
* No notation: the user converts the deadline to the local time

## Decision Outcome

Chosen option: "An offset from UTC after the time, `deadline:2026-11-30T23:59-12:00`", because it is the notation of ISO 8601 and RFC 3339, and the line keeps the deadline as it was announced.

The offset is `Z` or `+hh:mm` or `-hh:mm`; seconds may be written and are not used.
The application counts with the day and the time that the value is in the time zone of the device, and shows them.
This extends decision 0029: a time without an offset stays a local time.

### Consequences

* Good, because the conversion, with daylight saving time, is done by the application.
* Good, because a value with seconds and an offset is a date-time of RFC 3339.
* Bad, because the day of such a deadline depends on the time zone of the device: two devices in different zones show different days.
* Bad, because the label of such a deadline shows another text than the line holds.

## Pros and Cons of the Options

### The name of a time zone after the time, such as `Europe/Berlin`

* Good, because the user does not have to know the offset on the day of the deadline.
* Bad, because ISO 8601 and RFC 3339 have no such notation.
* Bad, because "anywhere on Earth" is no time zone with a name.

### No notation: the user converts the deadline to the local time

* Good, because nothing has to be built.
* Bad, because the conversion is done by hand and the line does not hold the deadline as it was announced.
