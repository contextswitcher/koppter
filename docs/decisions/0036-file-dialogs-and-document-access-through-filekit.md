---
status: accepted
date: 2026-11-16
decision-makers: Oliver Kopp
---

# File Dialogs and Document Access Through FileKit

`adr~file-dialogs-of-filekit~1`

Needs: impl

## Context and Problem Statement

The user chooses the todo.txt file in a dialog of the platform.
The desktop application showed the file dialog of AWT, and the Android application called the document picker itself and read the chosen document through the content resolver.
On Android, a single chosen file comes without the files next to it, so that notes, scheduled tasks and the configuration needed a git repository; a chosen folder gives access to all of them.
How does the application show these dialogs and access what the user chose?

## Considered Options

* The library FileKit
* The interfaces of each platform, called by the application

## Decision Outcome

Chosen option: "The library FileKit", because one library shows the dialogs for files and folders on all platforms and reads and writes the documents of a chosen folder on Android, which the application would otherwise write itself.

The desktop application and the Android application open the dialogs for a file and for a folder with FileKit.
On Android, the application keeps the access to what was chosen with a bookmark of FileKit and reads and writes the documents through its `PlatformFile`.
The common user interface does not depend on FileKit: each platform passes its choosers to it.

### Consequences

* Good, because Windows and macOS show their current dialogs, and Linux that of the desktop portal, with the dialogs of AWT and Swing where there is no portal.
* Good, because a folder chosen on Android has its notes, scheduled tasks, configuration and plan without a git repository.
* Bad, because the application depends on a library that has not reached version 1.0 and may change its interface.
* Bad, because the desktop application carries a client for D-Bus, and its packaged runtime one more module of Java.
* Bad, because the dialog of Windows for a file does not take the name of a file that does not exist yet; the dialog for a folder serves that case.
* Bad, because the access to a folder on Android is not verified on a device in the development sessions.

## Pros and Cons of the Options

### The interfaces of each platform, called by the application

* Good, because it needs no library.
* Good, because the application decides itself from which thread a dialog is shown, which the dialog of AWT needed on Windows.
* Bad, because the documents of a folder on Android are addressed differently by each provider, and the code for that is some hundred lines that the library has already.
* Bad, because the dialog of AWT is not the current one of the desktop on Linux.
