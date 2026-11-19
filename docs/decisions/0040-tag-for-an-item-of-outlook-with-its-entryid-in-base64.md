---
status: accepted
date: 2026-11-18
decision-makers: Oliver Kopp
---

# A Tag Names an Item of Outlook by Its EntryID in Base64

`adr~outlook-tag~1`

Needs: dsn

## Context and Problem Statement

An item of the To-Do list of Outlook becomes a line of the file when the user gives it a context or a project.
The line has to name the item, so that the item is not shown a second time and the user can open it.
Outlook names an item by its EntryID, 70 bytes that it writes as 140 hexadecimal digits, and by the StoreID of its account, about 300 more.
How does the line name the item?

## Considered Options

* The tag `ol:` with the EntryID in Base64 for URLs
* The tag `ol:` with the EntryID in hexadecimal digits
* The link of AnchorHub with EntryID and StoreID
* The tag `ol:` with a short id, and a file next to `todo.txt` that maps it to the EntryID

## Decision Outcome

Chosen option: "The tag `ol:` with the EntryID in Base64 for URLs", because it is the shortest form that needs nothing besides the line.

The value has 94 characters for an EntryID of 70 bytes, letters, digits, `-` and `_`.
The link to open the item is computed from it and names no store; AnchorHub looks for the EntryID in all stores.

### Consequences

* Good, because the line names the item without another file, and other todo.txt tools keep the tag.
* Good, because the same item gives the same tag on every computer.
* Bad, because the tag is still long and says nothing to a reader; the application shows it as the label *Outlook*.
* Bad, because the EntryID changes when the item moves to another store, such as an archive, and the tag then names nothing; this holds for every option but the short id, whose file would have to be updated instead.

## Pros and Cons of the Options

### The tag `ol:` with the EntryID in hexadecimal digits

* Good, because it is what Outlook and AnchorHub print.
* Bad, because it is half as long again as Base64.

### The link of AnchorHub with EntryID and StoreID

* Good, because Outlook finds the item without looking through the stores.
* Bad, because the line gets about 450 characters longer.

### The tag `ol:` with a short id, and a file next to `todo.txt` that maps it to the EntryID

* Good, because the line stays short.
* Bad, because the line is useless without the file, and both have to be synchronized together.
* Bad, because a short id may be taken twice, from two computers, unless it is derived from the EntryID.
