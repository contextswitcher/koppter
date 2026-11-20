---
status: accepted
date: 2026-11-20
decision-makers: Oliver Kopp
---

# Watch Fed by the Phone Through the Data Layer

`adr~watch-through-data-layer~1`

Needs: dsn, impl

## Context and Problem Statement

A watch with Wear OS, such as a Galaxy Watch from the fourth generation on, runs Android applications.
The user wants to see there what is planned next and to complete a task without taking out the phone.
The todo.txt file is the data store, and it lies on the phone: in a git clone, or behind a document provider such as that of OneDrive.
How does the watch get the tasks, and how does a completion get into the file?

## Considered Options

* An application for the watch that the phone application feeds through the Data Layer of Wear OS
* An application for the watch with a git clone of its own
* The notifications of the phone only

## Decision Outcome

Chosen option: "An application for the watch that the phone application feeds through the Data Layer of Wear OS", because it works for every place of the file that the phone application reads, and the file keeps a single writer on the two devices.

The phone application stores the agenda, which is what its widget shows, as an item of the Data Layer, and the watch application shows it.
The watch application sends a message to complete a task, and the phone application changes the file as if the user had done it there.
The notifications of the phone come in addition, as they cost the watch nothing.

### Consequences

* Good, because the watch application neither reads a file nor synchronizes: it has no settings and no key.
* Good, because the watch shows the last agenda also while the phone is out of reach.
* Bad, because the phone application depends on the client library of Google Play services, which is not open source, and the watch works with phones that have these services only.
* Bad, because both applications need the same application id and the same signature, so that a build of one's own does not talk to a released one.
* Bad, because the two applications are installed apart: the names of the items and messages stay as they are once released, and what is added has to be optional.
* Bad, because a completion needs the phone in reach.
* Bad, because the watch application is not run on a device or an emulator in the development sessions.

## Pros and Cons of the Options

### An application for the watch with a git clone of its own

* Good, because the watch needs no phone, also for completing a task.
* Bad, because it works for a file in a git repository only, not for one behind a document provider.
* Bad, because the user sets up a key for the repository on a device without a keyboard.
* Bad, because fetching and pushing over the radio of a watch costs its battery.

### The notifications of the phone only

* Good, because it needs no application on the watch and no library.
* Bad, because the watch shows the block that starts and nothing else: not what follows, and not the list of tasks.
