---
status: accepted
date: 2026-11-21
decision-makers: Oliver Kopp
---

# Absences of a Member as Blocks in the calendar.txt of the List

`adr~absences-in-calendar-txt~1`

Needs: dsn

## Context and Problem Statement

The members of a family have calendars of their own, such as that of an employer or of a school, which a service publishes as an iCalendar file at an address that is a secret.
The family view is to show when a member is not there, on every device of the family and on a tablet that all of them use, but not what the member does at that time.
How do the times of these calendars reach the family view?

## Considered Options

* The device that knows the address writes the times as blocks named `Absent` into the `calendar.txt` of the list of the member
* Every device reads the calendars itself, from addresses that are kept in the repository of the family
* Every device reads the calendars itself, from addresses that are entered on each device
* The events are copied into the `calendar.txt` with their titles

## Decision Outcome

Chosen option: "The device that knows the address writes the times as blocks named `Absent` into the `calendar.txt` of the list of the member", because the address and the titles of the events stay on a device of the member, and the other devices need nothing but the repository that they synchronize anyway.

The update replaces the blocks named `Absent` from today on and leaves the other blocks, the texts and the earlier days of the file as they are, so that a family writes its own events into the same file.

### Consequences

* Good, because a tablet in the hallway shows the absences without holding an address of a calendar.
* Good, because the file is `calendar.txt` as other tools read it.
* Bad, because the absences are as current as the last run of the application on the device that knows the address.
* Bad, because every change of a calendar is a commit in the repository of the family.
* Bad, because a block that somebody names `Absent` by hand is removed by the next update.

## Pros and Cons of the Options

### Every device reads the calendars itself, from addresses that are kept in the repository of the family

* Good, because the absences are current on every device.
* Bad, because everybody who reads the repository reads the addresses, and with them the whole calendars.

### Every device reads the calendars itself, from addresses that are entered on each device

* Good, because nothing is written into the repository.
* Bad, because every address is entered on every device, the shared tablet included, which then holds the calendars of all members.

### The events are copied into the `calendar.txt` with their titles

* Good, because the family sees why a member is not there.
* Bad, because a calendar of an employer holds titles that are not for the family, and the member cannot choose per event.
