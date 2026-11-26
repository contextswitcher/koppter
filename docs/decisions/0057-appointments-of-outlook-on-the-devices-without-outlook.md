---
status: accepted
date: 2026-11-26
decision-makers: Oliver Kopp
---

# Appointments of Outlook on the Devices without Outlook

`adr~outlook-on-other-devices~1`

Needs: dsn

## Context and Problem Statement

The application reads the calendar of Outlook through AnchorHub, which runs on the desktop with the classic Outlook only.
A phone therefore shows the plan without the appointments, and a plan made on the phone puts tasks into the times of meetings, which then replaces the plan of the desktop in `calendar.txt`.
How do the appointments of Outlook reach the devices without Outlook?

## Considered Options

* A copy of the appointments in a file `calendar-outlook.txt` next to the todo.txt file, written by the desktop when it plans
* The calendar of Outlook published as an iCalendar file and named under the calendars with busy times, whose events the calendar views already show

## Decision Outcome

Chosen option: "A copy of the appointments in a file `calendar-outlook.txt`", in addition to the published calendar, because an employer may forbid publishing the calendar, while the copy needs nothing but the synchronization that the todo.txt file has anyway.

The copy has the format of calendar.txt, so that a text editor reads it and `CalendarTxt` parses it.
It is written only when the user turns it on in the settings, as it puts the subjects of the appointments into the repository.

### Consequences

* Good, because a plan made on the phone keeps the times of the appointments free.
* Bad, because the copy is as old as the last plan of the desktop, and it lacks the colors of the categories and the appointments on the sideline.
* Bad, because the subjects of the appointments are in the history of the repository once the copy is turned on.
* Bad, because a calendar that is both copied and published shows each appointment twice on a device that reads both.

## Pros and Cons of the Options

### A copy in calendar-outlook.txt

* Good, because it works where publishing the calendar is not allowed and without network on the phone.
* Bad, because it is as current as the last plan of the desktop.

### The published calendar of Outlook

* Good, because it is current and needs no desktop running.
* Bad, because it needs the permission to publish the calendar, and its address is a secret on every device.
