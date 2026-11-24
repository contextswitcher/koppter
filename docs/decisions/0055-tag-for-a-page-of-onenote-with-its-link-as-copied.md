---
status: accepted
date: 2026-11-24
decision-makers: Oliver Kopp
---

# Tag for a Page of OneNote with its Link as Copied

`adr~onenote-tag~1`

Needs: dsn

## Context and Problem Statement

*Copy Link to Page* in OneNote puts two lines on the clipboard: an address of `onedrive.live.com` for the browser and a link `onenote:https://d.docs.live.net/…` for the program, each several hundred characters long.
A task should lead to the page without its line becoming unreadable.
How does the line keep the link?

## Considered Options

* The link of OneNote as it is copied, read as the tag `onenote:`
* The link in Base64 or compressed, in a tag of its own
* Only the ids of the section and the page

## Decision Outcome

Chosen option: "The link of OneNote as it is copied, read as the tag `onenote:`", because pasting the clipboard gives the tag without any conversion, and the label hides the length in the application.

### Consequences

* Good, because other todo.txt tools and a text editor keep a link that OneNote opens as it stands.
* Good, because the name of the notebook and of the page can be read in the line.
* Bad, because the line in the file stays long.

## Pros and Cons of the Options

### The link in Base64 or compressed, in a tag of its own

* Good, because the tag has no characters that other tools could read as something else.
* Bad, because Base64 makes the link a third longer; compression saves about half, but the tag can no longer be read or searched, and pasting needs a conversion.

### Only the ids of the section and the page

* Good, because the tag is short.
* Bad, because a link of OneNote names the notebook by its path: from the ids alone, OneNote does not find the page.
