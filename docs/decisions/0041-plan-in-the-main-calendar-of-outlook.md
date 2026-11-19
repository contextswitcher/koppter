---
status: accepted
date: 2026-11-18
decision-makers: Oliver Kopp
---

# The Plan Goes Into the Main Calendar of Outlook, Marked by AnchorHub

`adr~plan-in-outlook~1`

Needs: dsn

## Context and Problem Statement

The plan is written to `calendar.ics`, which a calendar program imports once.
The user wants the blocks of the plan to block time in the calendar that colleagues see, and to keep that calendar current with each plan.
A new plan has to replace the blocks of the previous one, and removing the blocks must never touch another appointment.
Where do the blocks go, and how are they told apart from the other appointments?

## Considered Options

* Appointments in the main calendar through AnchorHub, recognized by a user property and a category that AnchorHub sets
* Appointments in a calendar of their own through AnchorHub
* Appointments in the main calendar, recognized by `[koppter]` at the end of the subject
* The import of `calendar.ics` by the user

## Decision Outcome

Chosen option: "Appointments in the main calendar through AnchorHub, recognized by a user property and a category that AnchorHub sets", because only the main calendar counts for the free and busy times that others see, and a property that the user cannot type cannot be set by mistake.

The appointments are private, so that others see the time as taken but not the task, and tentative, so that a meeting may still be put there.
The subject ends with `[koppter]` and the text starts with it, so that the user sees whose appointment it is; the subject decides nothing.

### Consequences

* Good, because colleagues see the planned time as tentatively taken.
* Good, because an appointment that the user created never counts as a block, whatever its subject says; the worst case of an error in the marks is a block that stays, not one that is lost.
* Good, because deleted blocks are in the deleted items of Outlook.
* Bad, because the blocks are written only on Windows with the classic Outlook and AnchorHub.
* Bad, because a published iCalendar file of the same calendar contains the blocks without their marks; it must not be among the calendars with busy times, as the blocks would then keep the next plan away from themselves.

## Pros and Cons of the Options

### Appointments in a calendar of their own through AnchorHub

* Good, because removing the blocks is removing the calendar.
* Bad, because a second calendar does not count for the free and busy times of the account.

### Appointments in the main calendar, recognized by `[koppter]` at the end of the subject

* Good, because the mark is visible and needs no hidden property.
* Bad, because an appointment that the user names that way, or copies from a block, would be deleted.

### The import of `calendar.ics` by the user

* Good, because it works with every calendar program.
* Bad, because each plan needs a manual import, and whether an import replaces the earlier events has not been tried.
