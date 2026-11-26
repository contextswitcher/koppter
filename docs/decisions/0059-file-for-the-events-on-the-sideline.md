---
status: accepted
date: 2026-11-26
decision-makers: Oliver Kopp
---

# File for the Events on the Sideline

`adr~sideline-file~1`

Needs: dsn

## Context and Problem Statement

An appointment of Outlook goes on the sideline by the category `Sideline`, which Outlook keeps.
The calendars with busy times are iCalendar files that the application only reads, so it cannot mark an event in them.
Words of the sideline in `koppter.yml` match every event with that title, not a single one.
Where does the application keep that a single event, for a repeating one all its occurrences, is on the sideline?

## Considered Options

* A file `calendar-sideline.txt` next to the todo.txt file, one UID per line with the title as a comment
* A key `sideline-events` in `koppter.yml` with the UIDs
* A setting of the device
* The title of the event as a word of the sideline

## Decision Outcome

Chosen option: "A file `calendar-sideline.txt` next to the todo.txt file, one UID per line with the title as a comment", because it is synchronized with the todo.txt file, so that every device plans the same way, its lines stand for themselves and are merged line by line, and it keeps a list that grows with every sidelined event out of the configuration that the user edits.

### Consequences

* Good, because two devices that sideline different events at the same time do not conflict.
* Good, because the title after the UID tells the reader which event a line means.
* Bad, because the lines of past events stay until the user deletes them.
* Bad, because an event that its calendar publishes again with a new UID is no longer on the sideline.

## Pros and Cons of the Options

### A key `sideline-events` in `koppter.yml` with the UIDs

* Good, because no new file is added.
* Bad, because `koppter.yml` is left to the merge of git, so two devices that sideline at the same time stop the synchronization.
* Bad, because the long UIDs of past events crowd the file that the user edits by hand.

### A setting of the device

* Good, because nothing is written next to the todo.txt file.
* Bad, because another device plans into the time of an event that this one keeps free, or the other way round.

### The title of the event as a word of the sideline

* Good, because it needs no new storage: the words exist already.
* Bad, because it also matches other events with the same title or a title that contains it.
