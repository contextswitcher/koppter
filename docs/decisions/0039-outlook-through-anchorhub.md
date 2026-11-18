---
status: accepted
date: 2026-11-18
decision-makers: Oliver Kopp
---

# The To-Do List of Outlook Is Read Through AnchorHub

`adr~outlook-through-anchorhub~1`

Needs: dsn, impl

## Context and Problem Statement

The user keeps flagged mails and tasks in the To-Do list of the classic desktop Outlook and wants to see them in the list of the application, and to open them in Outlook from there.
The classic Outlook offers its data through COM only, on Windows.
[AnchorHub](https://github.com/contextswitcher/anchorhub) already captures and opens items of Outlook over COM for other programs.
How does the application reach Outlook?

## Considered Options

* The command line of AnchorHub (`ankd outlook-todos`, `outlook-open`, `outlook-complete`)
* A PowerShell script of the application itself that calls COM
* Microsoft Graph

## Decision Outcome

Chosen option: "The command line of AnchorHub", because the access to Outlook and the form of the links to its items stay in one program, which other tools already use.

### Consequences

* Good, because the application needs no code for COM and runs on Windows without Outlook as before.
* Good, because a link to an item that AnchorHub writes elsewhere names the same item.
* Bad, because the items appear only where AnchorHub is installed.
* Bad, because a change of the output of AnchorHub breaks the reading; the application reads only the fields `entryId`, `subject` and `due`.
* Bad, because every reading starts a process and PowerShell, which takes seconds, so the list is read every two minutes and not at once.

## Pros and Cons of the Options

### A PowerShell script of the application itself that calls COM

* Good, because nothing else has to be installed.
* Bad, because the access to Outlook, with its workarounds for the type library of Click-to-Run, would exist twice.

### Microsoft Graph

* Good, because it also reaches the new Outlook and works on Android.
* Bad, because it needs an app registration and a sign-in, which an organization may forbid.
* Bad, because flagged mails of accounts that are not in Exchange Online are not reached.
