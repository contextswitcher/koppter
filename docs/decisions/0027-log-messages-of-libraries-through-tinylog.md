---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# Log Messages of Libraries Go Through tinylog

`adr~log-messages-through-tinylog~1`

Needs: impl

## Context and Problem Statement

JGit and its SSH client write their log messages through SLF4J.
The application has nothing that takes these messages, so that SLF4J complains with three lines at every start of the desktop application and drops what the libraries report, warnings and errors of the synchronization included.
What takes the log messages of the libraries?

## Considered Options

* tinylog
* The NOP binding of SLF4J
* The simple binding of SLF4J
* Logback

## Decision Outcome

Chosen option: "tinylog", because it takes the messages of SLF4J with two small libraries, is configured by a short properties file, and writes to a file without another library.

The desktop application writes messages from the level of a warning on to the console.
It writes messages from the level of an information on to a file in the directory `logs` of its data directory, which is kept to three files of one megabyte.
The Android application does the same, with logcat in place of the console and the file in the directory of the application in the shared storage.
The application writes its own failures through the interface of tinylog.

### Consequences

* Good, because the start is free of the complaint of SLF4J, and a warning of JGit is no longer lost.
* Good, because a user who reports a problem of the synchronization can send a log file.
* Bad, because the log file can name the address of the git repository of the user, who has to look at it before sending it.
* Good, because one library serves the desktop and Android: it has a writer for logcat.
* Bad, because the packages carry three more libraries.
* Bad, because the log on Android is not verified on a device in the development sessions.

## Pros and Cons of the Options

### The NOP binding of SLF4J

* Good, because it is the smallest addition that silences the complaint.
* Bad, because the warnings and errors of the libraries stay lost.

### The simple binding of SLF4J

* Good, because it is maintained together with SLF4J and writes to the console without configuration.
* Bad, because it writes to the console only, so that a log file would need another library later.

### Logback

* Good, because it is the most widely used implementation and has every kind of output.
* Bad, because it is several times the size and is configured in XML, for an application that only passes on the messages of two libraries.
